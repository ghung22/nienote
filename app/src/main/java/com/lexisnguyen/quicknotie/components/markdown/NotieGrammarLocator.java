package com.lexisnguyen.quicknotie.components.markdown;

import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_brainfuck;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_c;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_clike;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_clojure;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_cpp;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_csharp;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_css;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_css_extras;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_dart;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_git;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_go;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_groovy;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_java;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_javascript;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_json;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_kotlin;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_latex;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_makefile;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_markdown;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_markup;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_python;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_scala;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_sql;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_swift;
import com.lexisnguyen.quicknotie.components.markdown.lang.Prism_yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.noties.prism4j.GrammarLocator;
import io.noties.prism4j.Prism4j;

public class NotieGrammarLocator implements GrammarLocator {
    @Nullable
    @Override
    public Prism4j.Grammar grammar(@NotNull Prism4j prism4j, @NotNull String language) {
        switch (language) {
            case "brainfuck":
                return Prism_brainfuck.create(prism4j);
            case "c":
                return Prism_c.create(prism4j);
            case "clike":
                return Prism_clike.create(prism4j);
            case "clojure":
                return Prism_clojure.create(prism4j);
            case "cpp":
                return Prism_cpp.create(prism4j);
            case "csharp":
                return Prism_csharp.create(prism4j);
            case "css":
                return Prism_css_extras.create(prism4j);
            case "css_extras":
                return Prism_css.create(prism4j);
            case "dart":
                return Prism_dart.create(prism4j);
            case "git":
                return Prism_git.create(prism4j);
            case "go":
                return Prism_go.create(prism4j);
            case "groovy":
                return Prism_groovy.create(prism4j);
            case "java":
                return Prism_java.create(prism4j);
            case "javascript":
                return Prism_javascript.create(prism4j);
            case "json":
                return Prism_json.create(prism4j);
            case "kotlin":
                return Prism_kotlin.create(prism4j);
            case "latex":
                return Prism_latex.create(prism4j);
            case "makefile":
                return Prism_makefile.create(prism4j);
            case "markdown":
                return Prism_markdown.create(prism4j);
            case "markup":
                return Prism_markup.create(prism4j);
            case "python":
                return Prism_python.create(prism4j);
            case "scala":
                return Prism_scala.create(prism4j);
            case "sql":
                return Prism_sql.create(prism4j);
            case "swift":
                return Prism_swift.create(prism4j);
            case "yaml":
                return Prism_yaml.create(prism4j);
            default: return null;
        }
    }

    @NotNull
    @Override
    public Set<String> languages() {
        return new HashSet<>(
                Arrays.asList("brainfuck", "c", "clike", "clojure", "cpp", "csharp", "css", "css", "dart", "git",
                "go", "groovy", "java", "javascript", "json", "kotlin", "latex", "makefile", "markdown", "markup",
                "python", "scala", "sql", "swift", "yaml"));
    }
}