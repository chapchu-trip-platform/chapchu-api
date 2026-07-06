package com.pettrip.harness;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("하네스 가비지 컬렉션: 드리프트 방지 센서")
class DriftPreventionTest {

  // workingDir = rootProject.projectDir (app/build.gradle 설정)
  private static final Path ROOT_DIR = Paths.get(".");

  @Test
  @DisplayName("[구조 드리프트] 모듈 src 디렉토리 내에 임시 또는 백업 파일이 존재해서는 안 된다")
  void checkStructureDrift() throws IOException {
    List<String> forbiddenPatterns =
        List.of(".*temp_.*", ".*_new\\..*", ".*_old\\..*", ".*_backup\\..*", ".*\\.bak$");
    List<Path> violations = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(ROOT_DIR, 2)) {
      paths
          .filter(p -> p.getFileName().toString().equals("src") && Files.isDirectory(p))
          .forEach(
              srcDir -> {
                try (Stream<Path> srcPaths = Files.walk(srcDir)) {
                  srcPaths
                      .filter(Files::isRegularFile)
                      .forEach(
                          file -> {
                            String name = file.getFileName().toString().toLowerCase();
                            for (String pattern : forbiddenPatterns) {
                              if (name.matches(pattern)) violations.add(file);
                            }
                          });
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    assertThat(violations).as("X 허용되지 않는 임시/백업 파일이 발견되었습니다:\n%s", violations).isEmpty();
  }

  @Test
  @DisplayName("[문서 드리프트] AGENTS.md에 언급된 파일 경로는 실제로 존재해야 한다")
  void checkDocumentationDrift() throws IOException {
    Path agentsMd = ROOT_DIR.resolve("AGENTS.md");
    if (!Files.exists(agentsMd)) return;
    String content = Files.readString(agentsMd);
    Matcher matcher = Pattern.compile("`([a-zA-Z0-9_/-]+\\.[a-zA-Z0-9]+)`").matcher(content);
    List<String> missingFiles = new ArrayList<>();
    while (matcher.find()) {
      String filePath = matcher.group(1);
      if (filePath.contains("/") && !Files.exists(ROOT_DIR.resolve(filePath))) {
        missingFiles.add(filePath);
      }
    }
    assertThat(missingFiles)
        .as("X AGENTS.md에 명시되었으나 실제 존재하지 않는 파일입니다:\n%s", missingFiles)
        .isEmpty();
  }
}
