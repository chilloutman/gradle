/*
 * Copyright 2015 the original author or authors.
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

package sample.markdown
import org.gradle.api.Task
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.LanguageType
import org.gradle.platform.base.LanguageTypeBuilder
import sample.documentation.DocumentationBinary

// START SNIPPET markdown-lang-registration
// START SNIPPET markdown-tasks-generation
class MarkdownPlugin extends RuleSource {
// END SNIPPET markdown-tasks-generation
    @LanguageType
    void registerMarkdownLanguage(LanguageTypeBuilder<MarkdownSourceSet> builder) {
        builder.setLanguageName("Markdown")
    }
// END SNIPPET markdown-lang-registration

// START SNIPPET markdown-tasks-generation
    @BinaryTasks
    void processMarkdownDocumentation(ModelMap<Task> tasks, final DocumentationBinary binary) {
        binary.inputs.withType(MarkdownSourceSet) { markdownSourceSet ->
            def taskName = binary.tasks.taskName("compile", markdownSourceSet.name)
            def outputDir = new File(binary.outputDir, markdownSourceSet.name)
            tasks.create(taskName, MarkdownHtmlCompile) { compileTask ->
                compileTask.source = markdownSourceSet.source
                compileTask.destinationDir = outputDir
                compileTask.smartQuotes = markdownSourceSet.smartQuotes
                compileTask.generateIndex = markdownSourceSet.generateIndex
            }
        }
    }
// START SNIPPET markdown-lang-registration
}
// END SNIPPET markdown-lang-registration
// END SNIPPET markdown-tasks-generation
