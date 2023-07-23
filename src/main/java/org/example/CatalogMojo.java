package org.example;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Goal generates backstage catalog for the project
 */
@Mojo( name = "catalog")
public class CatalogMojo extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        List<Dependency> dependencies = project.getDependencies();
        Map<String, Object> results = new HashMap<>();
        results.put("apiVersion", "backstage.io/v1alpha1");
        results.put("kind","Component");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", project.getArtifactId());
        metadata.put("description", project.getDescription() != null? project.getDescription() : project.getName());
////        //TODO
        results.put("metadata", metadata);
        Map<String, Object> spec = new HashMap<>();
        spec.put("type", "service");
        spec.put("lifecycle", "production");
        spec.put("owner", project.getOrganization() != null? project.getOrganization().getName():project.getArtifactId());
        List<String> dList = new ArrayList<>();
        List<Object> finalData = new ArrayList<>();
        finalData.add(results);
        for(Dependency d : dependencies){
            dList.add("Component:third-party/" + d.getArtifactId() + "_" + d.getVersion());

            Map<String, Object> component = new HashMap<>();
            component.put("apiVersion", "backstage.io/v1alpha1");
            component.put("kind", "Component");
            Map<String, Object> cMetadata = new HashMap<>();
            cMetadata.put("name", d.getArtifactId() + "_" + d.getVersion());
            cMetadata.put("namespace", "third-party");
            cMetadata.put("description", d.getArtifactId() + " " + d.getVersion());
            component.put("metadata", cMetadata);
            Map<String, Object> cSpec = new HashMap<>();
            cSpec.put("type", "library");
            cSpec.put("lifecycle", "production");
            cSpec.put("owner", "external");
            component.put("spec", cSpec);

            finalData.add(component);
        }

        spec.put("dependsOn", dList);
        results.put("spec", spec);


        DumperOptions dop = new DumperOptions();
        dop.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        org.yaml.snakeyaml.Yaml y = new Yaml(dop);

        getLog().info(y.dumpAll(finalData.listIterator()));
        try {
            Path outFile = Paths.get(project.getBuild().getDirectory(), project.getArtifactId()+"-catalog-info.yaml");
            Files.write(outFile, y.dumpAll(finalData.listIterator()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
