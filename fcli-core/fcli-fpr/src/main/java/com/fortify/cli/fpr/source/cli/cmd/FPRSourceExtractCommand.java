/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fpr.source.cli.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.aviator.fpr.utils.XmlUtils;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "extract-source", aliases = {"source", "es"})
public class FPRSourceExtractCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Option(names = {"-f", "--output-dir"}, required = true, order = 2)
    private Path outputDir;

    @Override
    public JsonNode getJsonNode() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            // Read src-archive/index.xml to get file mappings
            Path indexPath = fprHandle.getPath("/src-archive/index.xml");
            if (!Files.exists(indexPath)) {
                throw new FcliSimpleException("FPR does not contain a source archive (src-archive/index.xml not found)");
            }

            Map<String, String> fileMap = parseSourceIndex(indexPath);
            Files.createDirectories(outputDir);

            int extracted = 0;
            for (var entry : fileMap.entrySet()) {
                String filePath = entry.getKey();
                String archivePath = entry.getValue();

                Path srcEntry = fprHandle.getPath("/" + archivePath);
                if (Files.exists(srcEntry)) {
                    Path target = outputDir.resolve(filePath);
                    // Zip-slip protection
                    if (!target.normalize().startsWith(outputDir.normalize())) {
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(srcEntry, target, StandardCopyOption.REPLACE_EXISTING);
                    extracted++;
                }
            }

            var node = MAPPER.createObjectNode();
            node.put("outputDir", outputDir.toString());
            node.put("totalFiles", fileMap.size());
            node.put("extractedFiles", extracted);
            node.put("__action__", extracted > 0 ? "EXTRACTED" : "NO_FILES");
            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error extracting source archive", e);
        }
    }

    private Map<String, String> parseSourceIndex(Path indexPath) throws IOException {
        var map = new LinkedHashMap<String, String>();
        try (InputStream is = Files.newInputStream(indexPath)) {
            var doc = XmlUtils.secureDocumentBuilder(false).parse(is);
            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                var elem = (Element) entries.item(i);
                String id = elem.getAttribute("key");
                String path = elem.getTextContent().trim();
                if (id != null && !id.isBlank() && !path.isBlank()) {
                    map.put(id, path);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliTechnicalException("Failed to parse src-archive/index.xml", e);
        }
        return map;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
