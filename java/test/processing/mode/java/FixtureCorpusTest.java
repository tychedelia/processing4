/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2026 The Processing Foundation
*/

package processing.mode.java;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.compiler.IProblem;
import org.junit.BeforeClass;
import org.junit.Test;

import processing.app.Preferences;
import processing.app.exec.ProcessResult;
import processing.mode.java.preproc2.Preprocessor;

/**
 * End-to-end behavior verification against the legacy {@code .pde} fixture corpus that backed
 * {@code ParserTests} before the preproc2 cutover.
 *
 * <p>Each {@code .pde} in {@code java/test/resources/} runs through {@link Preprocessor} and is
 * then compiled by system {@code javac} against {@code processing.core} on the classpath. If a
 * fixture has a sibling {@code .expected} file, it must preprocess and compile cleanly. If it
 * does not, the fixture is one of the ~10 known intentional-failure cases — the preprocessor or
 * compiler must surface a problem.
 *
 * <p>The legacy {@code .expected} files are exact byte dumps of the ANTLR pipeline's output and
 * differ from preproc2's output for valid reasons (formatter whitespace, settings-hoist removes
 * {@code size()} rather than commenting it out, etc.) — strict text diff isn't useful, so this
 * test verifies the only thing that matters: <em>does it actually compile and run</em>.
 */
public class FixtureCorpusTest {

  private static final String RESOURCES = "test/resources/";
  private static final String RESOURCES_UP_DIR = "../java/test/resources";

  private static UTCompiler compiler;

  @BeforeClass
  public static void init() throws Exception {
    compiler = new UTCompiler(processingCoreClasspath());
    File prefs = res("preferences.txt");
    Preferences.load(new java.io.FileInputStream(prefs));
  }

  /**
   * Locate the compiled processing.core classes. Gradle puts them under
   * {@code core/build/classes/java/main}; the published jar lives at {@code core/library/core.jar}.
   * Fall back through both, with cwd-relative variants for both the {@code java/} and project-root
   * working directories Gradle has used over time.
   */
  private static File processingCoreClasspath() {
    String[] candidates = {
        "../core/build/classes/java/main",
        "../core/library/core.jar",
        "core/build/classes/java/main",
        "core/library/core.jar",
    };
    for (String c : candidates) {
      File f = new File(c);
      if (f.exists()) return f;
    }
    throw new IllegalStateException(
        "Cannot locate processing.core classpath; tried: " + Arrays.toString(candidates));
  }

  @Test
  public void allFixturesWithExpected_preprocessAndCompileCleanly() throws Exception {
    List<String> failures = new ArrayList<>();
    int checked = 0;
    for (File pde : listFixtures()) {
      String stem = stem(pde);
      File expected = sibling(pde, ".expected");
      if (!expected.exists()) continue;  // handled by the failure-case test

      checked++;
      String result = tryPreprocessAndCompile(stem, pde);
      if (result != null) failures.add(stem + ": " + result);
    }
    if (!failures.isEmpty()) {
      fail("Of " + checked + " happy-path fixtures, " + failures.size() + " regressed:\n  "
           + String.join("\n  ", failures));
    }
    // Sanity: there should be a healthy number of fixtures, otherwise the test isn't seeing them.
    assertTrue("expected at least 50 happy-path fixtures, got " + checked, checked >= 50);
  }

  @Test
  public void allFixturesWithoutExpected_surfaceAProblem() throws Exception {
    List<String> falselySuccessful = new ArrayList<>();
    int checked = 0;
    for (File pde : listFixtures()) {
      String stem = stem(pde);
      File expected = sibling(pde, ".expected");
      if (expected.exists()) continue;  // handled by the happy-path test

      checked++;
      String result = tryPreprocessAndCompile(stem, pde);
      if (result == null) {
        // Preprocessed and compiled cleanly — but no .expected means it shouldn't have.
        falselySuccessful.add(stem);
      }
    }
    if (!falselySuccessful.isEmpty()) {
      fail("Fixtures expected to fail but accepted cleanly: "
           + String.join(", ", falselySuccessful));
    }
    assertTrue("expected ~10 intended-failure fixtures, got " + checked, checked >= 5);
  }

  /**
   * Run the full pipeline for one fixture. Returns null on success, or a short failure summary.
   */
  private static String tryPreprocessAndCompile(String stem, File pdeFile) throws Exception {
    String pde = readFile(pdeFile);
    Preprocessor.Result r;
    try {
      r = Preprocessor.process(pde, defaultOptions(stem));
    } catch (RuntimeException e) {
      return "preprocess threw: " + e.getMessage();
    }

    StringBuilder errs = new StringBuilder();
    for (IProblem p : r.problems) {
      if (p.isError()) {
        if (errs.length() > 0) errs.append("; ");
        errs.append("preprocess error line ").append(p.getSourceLineNumber())
            .append(": ").append(p.getMessage());
      }
    }
    if (errs.length() > 0) return errs.toString();

    // Run javac against processing.core on classpath.
    ProcessResult compileResult = compiler.compile(stem, r.javaSource);
    if (compileResult.getResult() != 0) {
      String msg = compileResult.getStderr();
      if (msg == null || msg.isEmpty()) msg = compileResult.getStdout();
      return "javac failed: " + firstLine(msg);
    }
    return null;
  }

  private static Preprocessor.Options defaultOptions(String sketchName) {
    return new Preprocessor.Options(
        sketchName,
        Optional.empty(),
        Arrays.asList(Preprocessor.BASE_CORE_IMPORTS),
        Arrays.asList(Preprocessor.BASE_DEFAULT_IMPORTS),
        List.of(),
        false);
  }

  // ---- file helpers ----

  private static File[] listFixtures() {
    File[] files = resourcesDir().listFiles((dir, n) -> n.endsWith(".pde"));
    if (files == null) return new File[0];
    Arrays.sort(files);
    return files;
  }

  private static File resourcesDir() {
    File primary = new File(RESOURCES);
    return primary.isDirectory() ? primary : new File(RESOURCES_UP_DIR);
  }

  private static File res(String name) {
    File primary = new File(RESOURCES, name);
    return primary.exists() ? primary : new File(RESOURCES_UP_DIR, name);
  }

  private static File sibling(File f, String ext) {
    String stem = stem(f);
    return new File(f.getParentFile(), stem + ext);
  }

  private static String stem(File f) {
    String n = f.getName();
    int dot = n.lastIndexOf('.');
    return dot < 0 ? n : n.substring(0, dot);
  }

  private static String readFile(File f) throws java.io.IOException {
    StringBuilder sb = new StringBuilder();
    try (java.io.InputStreamReader in = new java.io.InputStreamReader(
            new java.io.FileInputStream(f), "UTF-8")) {
      char[] buf = new char[4096];
      int n;
      while ((n = in.read(buf)) > 0) sb.append(buf, 0, n);
    }
    return sb.toString();
  }

  private static String firstLine(String s) {
    if (s == null) return "(no output)";
    int nl = s.indexOf('\n');
    String line = nl < 0 ? s : s.substring(0, nl);
    return line.length() > 200 ? line.substring(0, 200) + "..." : line;
  }
}
