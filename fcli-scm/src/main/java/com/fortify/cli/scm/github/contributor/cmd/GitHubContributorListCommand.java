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
package com.fortify.cli.scm.github.contributor.cmd;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperMixin;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.scm.github.cli.cmd.AbstractGitHubOutputCommand;
import com.fortify.cli.scm.github.cli.mixin.GitHubOutputHelperMixins;
import com.fortify.cli.scm.github.cli.util.GitHubPagingHelper;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = GitHubOutputHelperMixins.List.CMD_NAME)
public class GitHubContributorListCommand extends AbstractGitHubOutputCommand implements IUnirestJsonNodeSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubContributorListCommand.class);
    private static final DateTimePeriodHelper PERIOD_HELPER = new DateTimePeriodHelper(Period.DAYS);
    @Getter @Mixin private GitHubOutputHelperMixins.List outputHelper;
    @Option(names = "--last", defaultValue = "90d", paramLabel = "[x]d")
    private String lastPeriod;
    @Option(names = "--no-older", negatable = true) 
    private boolean includeOlder = true;
    @ArgGroup(exclusive = true)
    private GitHubOrgAndRepoOptions orgAndRepoOptions = new GitHubOrgAndRepoOptions();
    @Mixin private ProgressHelperMixin progressHelper;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ResultData resultData = new ResultData();
        if ( !orgAndRepoOptions.repositories.isEmpty() ) {
            orgAndRepoOptions.repositories.forEach(repo->collectDataForRepo(unirest, resultData, repo));
        } else if ( !orgAndRepoOptions.organizations.isEmpty() ) {
            orgAndRepoOptions.organizations.forEach(orgName->collectDataForOrg(unirest, resultData, orgName));
        } else {
            progressHelper.writeI18nProgress("loading.organizations");
            HttpRequest<?> req = unirest.get("/user/orgs");
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(org->collectDataForOrg(unirest, resultData, org.get("login").asText())));
        }
        progressHelper.clearProgress();
        resultData.getWarnings().forEach(LOG::warn);
        return resultData.getResults();
    }

    private void collectDataForOrg(UnirestInstance unirest, ResultData resultData, String orgName) {
        progressHelper.writeI18nProgress("loading.repositories", orgName);
        HttpRequest<?> req = unirest.get("/orgs/{org}/repos?type=all")
                .routeParam("org", orgName);
        GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
            .ifSuccess(r->r.getBody().forEach(repo->collectDataForRepo(unirest, resultData, new GitHubRepo(orgName, repo.get("name").asText()))));
    }
    
    private void collectDataForRepo(UnirestInstance unirest, ResultData resultData, GitHubRepo repo) {
        progressHelper.writeI18nProgress("loading.repository", repo.getFullName());
        String since = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(lastPeriod)
                .format(DateTimeFormatter.ISO_INSTANT);
        HttpRequest<?> req = getCommitsRequest(unirest, repo)
                .queryString("since", since);
        try {
            CollectedAuthors collectedAuthors = new CollectedAuthors(); 
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(commit->collectDataForCommit(resultData, collectedAuthors, repo, commit)));
            
            if ( includeOlder && collectedAuthors.isEmpty() ) {
                getCommitsRequest(unirest, repo).queryString("per_page", "1")
                    .asObject(JsonNode.class)
                    .ifSuccess(r->r.getBody().forEach(commit->collectDataForCommit(resultData, collectedAuthors, repo, commit)));
            }
        } catch ( UnexpectedHttpResponseException e ) {
            handleRepoDataFailure(e, resultData, repo);
        }
    }

    private GetRequest getCommitsRequest(UnirestInstance unirest, GitHubRepo repo) {
        return unirest.get("/repos/{org}/{repo}/commits")
                .routeParam("org", repo.getOrgName())
                .routeParam("repo", repo.getRepoName());
    }
    
    private void handleRepoDataFailure(UnexpectedHttpResponseException e, ResultData resultData, GitHubRepo repo) {
        String msg = "Error loading commit data for repository: "+repo.getFullName();
        resultData.getWarnings().add(msg);
        LOG.debug(msg, e);
    }
    
    private void collectDataForCommit(ResultData resultData, CollectedAuthors collectedAuthors, GitHubRepo repo, JsonNode commit) {
        ObjectNode author = getAuthor(commit);
        if ( !collectedAuthors.contains(author) ) {
            collectedAuthors.add(author);
            ObjectNode data = JsonHelper.getObjectMapper().createObjectNode();
            data.put("organizationName", repo.getOrgName());
            data.put("repositoryName", repo.getRepoName());
            data.put("repositoryFullName", repo.getFullName());
            data.set("author", author);
            data.put("lastCommit", JsonHelper.evaluateSpELExpression(commit, "commit?.author?.date", String.class));
            resultData.getResults().add(data);
        }
    }
    
    private ObjectNode getAuthor(JsonNode commit) {
        return JsonHelper.getObjectMapper().createObjectNode()
            .put("name", JsonHelper.evaluateSpELExpression(commit, "commit?.author?.name", String.class))
            .put("email", JsonHelper.evaluateSpELExpression(commit, "commit?.author?.email", String.class))
            .put("login", JsonHelper.evaluateSpELExpression(commit, "author?.login", String.class));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
    
    private final class CollectedAuthors {
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Set<String> emails = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Set<String> logins = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        
        public void add(ObjectNode author) {
            add(names, "name", author);
            add(emails, "email", author);
            add(logins, "login", author);
        }
        
        public boolean contains(ObjectNode author) {
            return contains(names, "name", author)
                || contains(emails, "email", author)
                || contains(logins, "login", author);
        }
        
        public boolean isEmpty() {
            return names.isEmpty() && emails.isEmpty() && logins.isEmpty();
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
    
    static final class GitHubOrgAndRepoOptions {
        @Option(names = {"--organizations", "--orgs"}, split=",")
        private List<String> organizations = new ArrayList<>();
        
        @Option(names = {"--repositories", "--repos"}, split=",", converter = GitHubRepoTypeConverter.class)
        private Set<GitHubRepo> repositories = new HashSet<>();
    }
    
    @Data @AllArgsConstructor
    static final class GitHubRepo {
        private String orgName;
        private String repoName;
        public String getFullName() {
            return String.format("%s/%s", orgName, repoName);
        }
    }
    
    static final class GitHubRepoTypeConverter implements ITypeConverter<GitHubRepo> {
        @Override
        public GitHubRepo convert(String value) throws Exception {
            String[] split = value.split("/");
            if ( split.length!=2 ) {
                throw new IllegalArgumentException("Repositories must be specified as <organization|account>/<repo>, current value: "+value);
            }
            return new GitHubRepo(split[0], split[1]);
        }  
    }
    
    @Data
    static final class ResultData {
        private ArrayNode results = JsonHelper.getObjectMapper().createArrayNode();
        private List<String> warnings = new ArrayList<>();  
    }
}
