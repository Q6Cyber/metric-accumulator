package io.bpoole6.accumulator.controller;

import io.bpoole6.accumulator.controller.response.ConfigurationResponse;
import io.bpoole6.accumulator.controller.response.ServiceDiscovery;
import io.bpoole6.accumulator.service.MetricService;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.service.MetricGroupConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/")
public class MetricsController implements MetricsControllerInterface{

  private MetricService metricService;
  private final MetricGroupConfiguration metricGroupConfiguration;
  public MetricsController(MetricService metricService,
      MetricGroupConfiguration metricGroupConfiguration) {
    this.metricService = metricService;
    this.metricGroupConfiguration = metricGroupConfiguration;
  }


  @Override
  public ModelAndView serverStatus(ModelMap map) {
    ConfigurationResponse response = new ConfigurationResponse();
    response.setConfiguration("\n"+this.metricGroupConfiguration.getFileContent());
    response.setConfigurationFile(this.metricGroupConfiguration.getConfigurationFile());
    map.addAttribute("config", response);
    return new ModelAndView("configuration", map);
  }
  @Override
  public ResponseEntity<Object> reloadConfig() throws InterruptedException {
    boolean reset = this.metricService.resetConfigs();
    if(reset) {
      return new ResponseEntity<>("configurations has been reloaded", HttpStatus.OK);
    }else {
      return new ResponseEntity<>("Failed to reload configuration", HttpStatus.EXPECTATION_FAILED);
    }
  }


  public ResponseEntity<Object> resetMetricGroup(@PathVariable("metricGroup") String metricGroup) throws InterruptedException {
    Group group = this.metricGroupConfiguration.getMetricGroups().get(metricGroup.toLowerCase());

    if( metricService.resetMetricGroup(group)) {
      return new ResponseEntity<>("Metrics reset for %s".formatted(metricGroup), HttpStatus.OK);
    }else {
      return new ResponseEntity<>("Failed to reset metrics for %s".formatted(metricGroup), HttpStatus.EXPECTATION_FAILED);
    }
  }



  public ResponseEntity<String> receiveMetrics(@PathVariable("metricGroup") String metricGroup, @RequestBody String metrics)
          throws IOException, InterruptedException {
    Optional<Group> contextMetricGroup = getMetricGroupFromSecurityContext();
    Group group = this.metricGroupConfiguration.getMetricGroups().get(metricGroup.toLowerCase());

    if (Objects.nonNull(group)) {
      if(contextMetricGroup.isPresent() && !group.equals(contextMetricGroup.get())) {
        return new ResponseEntity<>("Wrong api key for metric group: " + metricGroup,HttpStatus.UNAUTHORIZED);
      }

      this.metricService.modifyMetrics(metrics, group);
      return new ResponseEntity<>(HttpStatus.CREATED);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }


  public ResponseEntity<String> metrics(@PathVariable("metricGroup") String metricName) {
    Group group = this.metricGroupConfiguration.getMetricGroups().get(metricName.toLowerCase());
    if (Objects.isNull(group)) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    Optional<String> optSnapshot = metricService.getMetricSnapshot(group);
    if (optSnapshot.isPresent()) {
      return new ResponseEntity<>(optSnapshot.get(), HttpStatus.OK);
    }else{
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  public ResponseEntity<List<ServiceDiscovery>> serviceDiscovery() {
    List<ServiceDiscovery> list = this.metricGroupConfiguration.getNames().stream().map(i-> new ServiceDiscovery("metrics/" + i, metricGroupConfiguration.getHostAddress())).toList();

    return new ResponseEntity<>(list, HttpStatus.OK);
  }


  public ResponseEntity<ConfigurationResponse> currentConfigurations(){
    ConfigurationResponse response = new ConfigurationResponse();
    response.setConfiguration(this.metricGroupConfiguration.getFileContent());
    response.setConfigurationFile(this.metricGroupConfiguration.getConfigurationFile());
    return new ResponseEntity<>(response,HttpStatus.OK);
  }

  public Optional<Group> getMetricGroupFromSecurityContext() {
    if(SecurityContextHolder.getContext().getAuthentication().getCredentials() instanceof Group){
      return Optional.of((Group) SecurityContextHolder.getContext().getAuthentication().getCredentials());
    }
    return Optional.empty();
  }


}
