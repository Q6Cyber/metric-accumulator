package io.bpoole6.accumulator;

import io.bpoole6.accumulator.service.MetricGroupConfiguration;
import io.bpoole6.accumulator.service.MetricManager;
import io.bpoole6.accumulator.service.MetricService;
import io.bpoole6.accumulator.service.RegistryRepository;
import io.bpoole6.accumulator.service.metricgroup.Group;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) //In the future we may want to just reset the registries and maps between tests
public class ResetConfigurationTest {

  @Autowired
  private RegistryRepository registryRepository;

  @Autowired
  private MetricService metricService;

  @Autowired
  private ScheduledTasks scheduledTasks;

  @Autowired
  private MetricGroupConfiguration metricGroupConfiguration;

  @Test
  void testMetricReset() throws IOException, InterruptedException {

    String oldName = "oldName";
    String newName = "newName";
    Path path = Paths.get(this.metricGroupConfiguration.getConfigurationFile());
    Files.writeString(path, TestUtils.metricsFile(oldName));
    this.metricService.resetConfigs();

    Optional<Group> oldGroup = this.registryRepository.getRegistryMap().keySet().stream().findFirst();
    Assertions.assertTrue(oldGroup.isPresent(), "Assert that registrymap has the current");
    Assertions.assertEquals(oldName, oldGroup.get().getName(), "Assert that Group name is %s".formatted(oldName));

    Files.writeString(path, TestUtils.metricsFile(newName));
    this.metricService.resetConfigs();

    Optional<Group> newGroup = this.registryRepository.getRegistryMap().keySet().stream().findFirst();
    Assertions.assertTrue(newGroup.isPresent(), "Assert that registrymap has the current");
    Assertions.assertEquals(newName, newGroup.get().getName(), "Assert that Group name is %s".formatted(newName));
  }


  @Test
  public void resetMetricManager() throws InterruptedException, IOException {
    String metrics = """
				# HELP python_gc_objects_collected_seconds Objects collected during gc
				# TYPE python_gc_objects_collected_seconds gauge
				python_gc_objects_collected_seconds{generation="0",_metrics_accumulator_latest="3992623250"} 4911.0
				""";
    Group group = this.registryRepository.getRegistryMap().keySet().stream().findFirst().get();
    this.metricService.modifyMetrics(metrics, group);

    MetricManager oldManager = this.registryRepository.getRegistryMap().values().stream().findFirst().get();
    Assertions.assertEquals(1, oldManager.getPrometheusRegistry().getPrometheusRegistry().scrape().size());
    oldManager.resetRegistries();
    Assertions.assertEquals(0, oldManager.getPrometheusRegistry().getPrometheusRegistry().scrape().size());

  }
  @Test
  public void testSchedulingReload(){
//    this.scheduledTasks.
  }
}
