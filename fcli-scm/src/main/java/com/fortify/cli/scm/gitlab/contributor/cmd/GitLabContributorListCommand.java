/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.scm.gitlab.contributor.cmd;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperMixin;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.scm.gitlab.cli.cmd.AbstractGitLabJsonNodeOutputCommand;
import com.fortify.cli.scm.gitlab.cli.mixin.AbstractGitLabProjectProcessorMixin;
import com.fortify.cli.scm.gitlab.cli.util.GitLabPagingHelper;
import com.fortify.cli.scm.gitlab.helper.GitLabProjectDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class GitLabContributorListCommand extends AbstractGitLabJsonNodeOutputCommand {
    private static final Logger LOG = LoggerFactory.getLogger(GitLabContributorListCommand.class);
    private static final DateTimePeriodHelper PERIOD_HELPER = new DateTimePeriodHelper(Period.DAYS);
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private ProgressHelperMixin progressHelper;
    @Mixin private GitLabContributorProcessor contributorProcessor = new GitLabContributorProcessor();

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        contributorProcessor.process(unirest, progressHelper);
        return contributorProcessor.finish(progressHelper);
    }

    private static final class GitLabContributorProcessor extends AbstractGitLabProjectProcessorMixin {
        @Option(names = "--last", defaultValue = "90d", paramLabel = "[x]d")
        private String lastPeriod;
        @Option(names = "--no-older", negatable = true) 
        private boolean includeOlder = true;
        
        @Getter private ResultData resultData = new ResultData();
        
        @Override
        protected void processProject(UnirestInstance unirest, ProgressHelperMixin progressHelper, JsonNode projectNode) {
            GitLabProjectDescriptor projectDescriptor = JsonHelper.treeToValue(projectNode, GitLabProjectDescriptor.class);
            progressHelper.writeI18nProgress("loading.project", projectDescriptor.getProjectFullPath());
            String since = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(lastPeriod)
                    .format(DateTimeFormatter.ISO_INSTANT);
            HttpRequest<?> req = getCommitsRequest(unirest, projectDescriptor)
                    .queryString("since", since);
            try {
                CollectedAuthors collectedAuthors = new CollectedAuthors(); 
                GitLabPagingHelper.pagedRequest(req, ArrayNode.class)
                    .ifSuccess(r->r.getBody().forEach(commit->collectDataForCommit(resultData, collectedAuthors, projectDescriptor, commit)));
                
                if ( includeOlder && collectedAuthors.isEmpty() ) {
                    getCommitsRequest(unirest, projectDescriptor).queryString("per_page", "1")
                        .asObject(JsonNode.class)
                        .ifSuccess(r->r.getBody().forEach(commit->collectDataForCommit(resultData, collectedAuthors, projectDescriptor, commit)));
                }
            } catch ( UnexpectedHttpResponseException e ) {
                handleRepoDataFailure(e, resultData, projectDescriptor);
            }
        }
        
        private GetRequest getCommitsRequest(UnirestInstance unirest, GitLabProjectDescriptor descriptor) {
            return unirest.get("/api/v4/projects/{id}/repository/commits")
                    .routeParam("id", descriptor.getProjectId());
        }
        
        private void handleRepoDataFailure(UnexpectedHttpResponseException e, ResultData resultData, GitLabProjectDescriptor repoDescriptor) {
            String msg = "Error loading commit data for project: "+repoDescriptor.getProjectFullPath();
            resultData.getWarnings().add(msg);
            LOG.debug(msg, e);
        }
        
        private void collectDataForCommit(ResultData resultData, CollectedAuthors collectedAuthors, GitLabProjectDescriptor projectDescriptor, JsonNode commit) {
            ObjectNode author = getAuthor(commit);
            if ( !collectedAuthors.contains(author) ) {
                collectedAuthors.add(author);
                ObjectNode data = JsonHelper.getObjectMapper().createObjectNode();
                data.setAll((ObjectNode)JsonHelper.getObjectMapper().valueToTree(projectDescriptor));
                data.set("author", author);
                data.put("lastCommit", JsonHelper.evaluateSpELExpression(commit, "authored_date", String.class));
                resultData.getResults().add(data);
            }
        }
        
        private ObjectNode getAuthor(JsonNode commit) {
            return JsonHelper.getObjectMapper().createObjectNode()
                .put("name", JsonHelper.evaluateSpELExpression(commit, "author_name", String.class))
                .put("email", JsonHelper.evaluateSpELExpression(commit, "author_email", String.class));
        }
        
        protected ArrayNode finish(ProgressHelperMixin progressHelper) {
            progressHelper.clearProgress();
            resultData.getWarnings().forEach(LOG::warn);
            return resultData.getResults();
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
    
    private static final class CollectedAuthors {
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Set<String> emails = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        
        public void add(ObjectNode author) {
            add(names, "name", author);
            add(emails, "email", author);
        }
        
        public boolean contains(ObjectNode author) {
            return contains(names, "name", author)
                || contains(emails, "email", author);
        }
        
        public boolean isEmpty() {
            return names.isEmpty() && emails.isEmpty();
        }

        private final void add(Set<String> set, String field, ObjectNode author) {
            JsonNode val = author.get(field);
            if ( val!=null && val.asText()!=null ) {
                set.add(val.asText());
            }
        }
        
        private final boolean contains(Set<String> set, String field, ObjectNode author) {
            JsonNode val = author.get(field);
            return val==null || val.asText()==null ? false : set.contains(val.asText());
        }
    } 
    
    @Data
    private static final class ResultData {
        private ArrayNode results = JsonHelper.getObjectMapper().createArrayNode();
        private List<String> warnings = new ArrayList<>();  
    }
}
