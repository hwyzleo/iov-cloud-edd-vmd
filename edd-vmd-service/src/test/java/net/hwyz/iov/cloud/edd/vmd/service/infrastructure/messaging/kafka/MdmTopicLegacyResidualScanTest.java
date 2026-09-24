package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旧 MDM Topic 名称残留扫描（VMD-DSN-CR-052 §10 验收）
 * <p>
 * 断言代码（main + test）与生效配置（application.yml 非注释行）中
 * 不存在任何未登记的旧 MDM Topic 名称或旧 Topic 配置键，防止迁移后回潮。
 * 保留项：{@code mdm.sync.*.kafka.enabled} 为消费者开关（非 Topic 名称）；\n
 * application.yml 中的迁移映射注释为登记文档，允许保留。\n
 * 本测试自身携带的旧名清单为断言数据，扫描时排除本文件。
 *
 * @author hwyz_leo
 */
@DisplayName("旧 MDM Topic 名称残留扫描")
class MdmTopicLegacyResidualScanTest {

    /** 旧 MDM Topic 名称（已迁移至 vmd.kafka.topics.mdm.*） */
    private static final List<String> LEGACY_TOPIC_NAMES = List.of(
            "mdm.product.brand.",
            "mdm.product.carLine.",
            "mdm.product.platform.",
            "mdm.org.plant.event",
            "mdm.product.model.",
            "mdm.product.variant.",
            "mdm.product.configuration.",
            "mdm.product.optionFamily.",
            "mdm.eead.vehicleNode.event",
            "mdm.material.part.event");

    /** 旧 Topic 配置键（created/updated/deactivated-topic 与 kafka.topic） */
    private static final Pattern LEGACY_TOPIC_KEY =
            Pattern.compile("mdm\\.sync\\.[a-z-]+\\.kafka\\.(created-topic|updated-topic|deactivated-topic|topic)");

    @Test
    @DisplayName("Java 源码（main + test）无旧 MDM Topic 名称/配置键")
    void javaSourcesHaveNoLegacyMdmTopicNames() throws IOException {
        List<String> hits = new ArrayList<>(scanDir(Paths.get("src/main/java")));
        hits.addAll(scanDir(Paths.get("src/test/java")));
        assertTrue(hits.isEmpty(), "发现旧 MDM Topic 名称/配置键残留:\n" + String.join("\n", hits));
    }

    @Test
    @DisplayName("application.yml 生效配置（非注释行）无旧 MDM Topic 名称/配置键")
    void activeYamlHasNoLegacyMdmTopicNames() throws IOException {
        Path yml = Paths.get("src/main/resources/application.yml");
        if (!Files.exists(yml)) {
            return;
        }
        for (String line : Files.readAllLines(yml)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            for (String name : LEGACY_TOPIC_NAMES) {
                assertTrue(!trimmed.contains(name),
                        "application.yml 生效配置行包含旧名称 '" + name + "': " + trimmed);
            }
            assertTrue(!LEGACY_TOPIC_KEY.matcher(trimmed).find(),
                    "application.yml 生效配置行包含旧 Topic 配置键: " + trimmed);
        }
    }

    private List<String> scanDir(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("MdmTopicLegacyResidualScanTest.java"))
                    .flatMap(p -> {
                        try {
                            return Files.readAllLines(p).stream()
                                    .filter(line -> matchesLegacy(line))
                                    .map(line -> p + ":" + line.trim());
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .toList();
        }
    }

    private boolean matchesLegacy(String line) {
        return LEGACY_TOPIC_NAMES.stream().anyMatch(line::contains)
                || LEGACY_TOPIC_KEY.matcher(line).find();
    }
}
