/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.plugins
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources
import org.gradle.internal.os.OperatingSystem
import org.gradle.test.fixtures.archive.ArchiveTestFixture
import org.gradle.test.fixtures.archive.TarTestFixture
import org.gradle.test.fixtures.archive.ZipTestFixture
import org.junit.Rule

import static org.gradle.play.integtest.fixtures.Repositories.PLAY_REPOSITORIES

class PlayDistributionPluginIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    public final TestResources resources = new TestResources(temporaryFolder)

    def setup() {
        settingsFile << """ rootProject.name = 'dist-play-app' """
        buildFile << """
            plugins {
                id 'play'
            }

            ${PLAY_REPOSITORIES}
        """
    }

    def "uses unique names for jars in distribution"() {
        given:
        file("extralib.jar").text = "This is not a jar"
        (1..2).each { subprojectIdx ->
            def subprojectName = "sub${subprojectIdx}"
            settingsFile << """
                include '${subprojectName}:dependency'
            """
            def dependencyRoot = file("${subprojectName}/dependency")
            def srcDir = dependencyRoot.file("src/main/java/${subprojectName}")
            srcDir.mkdirs()
            srcDir.file("Dependency.java") << """
                package ${subprojectName};
                public class Dependency {}"""
            dependencyRoot.file("build.gradle") << """
                apply plugin: 'java'
                group = 'com.example.${subprojectName}'
                version = "1.0"
"""
            buildFile << """
                dependencies {
                    playRun project(":${subprojectName}:dependency")
                }
"""
        }
        buildFile << """
            dependencies {
                playRun 'com.google.code.gson:gson:2.2.4'
                playRun files('extralib.jar')
            }
"""
        when:
        succeeds "dist"

        then:
        executedAndNotSkipped(":createPlayBinaryZipDist", ":createPlayBinaryTarDist")

        archives()*.containsDescendants(
            "playBinary/lib/extralib.jar",
            "playBinary/lib/com.google.code.gson-gson-2.2.4.jar",
            "playBinary/lib/sub1.dependency-dependency-1.0.jar",
            "playBinary/lib/sub2.dependency-dependency-1.0.jar")

        when:
        file("sub1/dependency/build.gradle") << "version = '2.0'"
        and:
        succeeds "dist"
        then:
        executedAndNotSkipped(":createPlayBinaryZipDist", ":createPlayBinaryTarDist")

        archives().each { archive ->
            archive.doesNotContainDescendants(
                "playBinary/lib/sub1.dependency-dependency-1.0.jar"
            ).containsDescendants(
                "playBinary/lib/sub1.dependency-dependency-2.0.jar",
                "playBinary/lib/sub2.dependency-dependency-1.0.jar"
            )
        }
    }

    def "builds a tgz when requested"() {
        given:
        buildFile << """
            model {
                tasks.createPlayBinaryTarDist {
                    compression = Compression.GZIP
                }
            }
        """
        when:
        succeeds ":createPlayBinaryTarDist"
        then:
        tar("build/distributions/playBinary.tgz").containsDescendants(
            "playBinary/lib/dist-play-app.jar",
            "playBinary/lib/dist-play-app-assets.jar",
            "playBinary/bin/playBinary",
            "playBinary/bin/playBinary.bat"
        )

    }
    def "builds empty distribution when no sources present" () {
        buildFile << """
            model {
                tasks.createPlayBinaryStartScripts {
                    doLast {
                        assert classpath.contains(file(createPlayBinaryDistributionJar.archivePath))
                    }
                }
            }
        """

        when:
        succeeds "stage"

        then:
        executedAndNotSkipped(
                ":createPlayBinaryJar",
                ":createPlayBinaryDistributionJar",
                ":createPlayBinaryAssetsJar",
                ":createPlayBinaryStartScripts",
                ":stagePlayBinaryDist")
        skipped(":compilePlayBinaryScala")
        notExecuted(
                ":compilePlayBinaryPlayRoutes",
                ":compilePlayBinaryPlayTwirlTemplates")

        and:
        file("build/stage/playBinary").assertContainsDescendants(
                "lib/dist-play-app.jar",
                "lib/dist-play-app-assets.jar",
                "bin/playBinary",
                "bin/playBinary.bat"
        )
        if (OperatingSystem.current().linux || OperatingSystem.current().macOsX) {
            assert file("build/stage/playBinary/bin/playBinary").mode == 0755
        }

        when:
        succeeds "dist"

        then:
        executedAndNotSkipped(":createPlayBinaryZipDist", ":createPlayBinaryTarDist")
        skipped(
                ":compilePlayBinaryScala",
                ":createPlayBinaryJar",
                ":createPlayBinaryDistributionJar",
                ":createPlayBinaryAssetsJar",
                ":createPlayBinaryStartScripts")
        notExecuted(
                ":compilePlayBinaryPlayRoutes",
                ":compilePlayBinaryPlayTwirlTemplates")

        and:
        archives()*.containsDescendants(
                "playBinary/lib/dist-play-app.jar",
                "playBinary/lib/dist-play-app-assets.jar",
                "playBinary/bin/playBinary",
                "playBinary/bin/playBinary.bat"
        )
    }

    TarTestFixture tar(String path) {
        return new TarTestFixture(file(path))
    }

    ZipTestFixture zip(String path) {
        return new ZipTestFixture(file(path))
    }

    List<ArchiveTestFixture> archives() {
        return [ zip("build/distributions/playBinary.zip"), tar("build/distributions/playBinary.tar") ]
    }
}
