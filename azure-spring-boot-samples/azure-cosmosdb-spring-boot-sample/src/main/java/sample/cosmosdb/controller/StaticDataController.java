package sample.cosmosdb.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.bind.annotation.*;
import sample.cosmosdb.model.FixAccountMap;
import sample.cosmosdb.repository.FixAccountMapRepository;
import sample.cosmosdb.repository.SernovaMappableRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@RestController
public class StaticDataController {

    private final FixAccountMapRepository fixAccountMapRepository;

    private final Map<String, SernovaMappableRepository> mappableRepositories;

    @Autowired
    public StaticDataController(FixAccountMapRepository repository, List<SernovaMappableRepository> mappableRepositories) {
        this.fixAccountMapRepository = repository;
        this.mappableRepositories = mappableRepositories.stream().collect(toMap(SernovaMappableRepository::getRepositoryName, v -> v));
    }

    @GetMapping("/allMappings/{mappingName}")
    public List allMappings(@PathVariable String mappingName) {
        getRepository(mappingName);
        return new ArrayList();
    }

    private SernovaMappableRepository getRepository(String mappingName) {
        if (mappableRepositories.containsKey(mappingName)) {
            return mappableRepositories.get(mappingName);
        }
        else {
            throw new UnsupportedOperationException("The mapping name requested does not exist: " + mappingName + "Registered mappings are: " +
                    mappableRepositories.values().stream()
                            .map(SernovaMappableRepository::getRepositoryName).collect(Collectors.joining(",")));
        }
    }

    @GetMapping("/allMappingsAsCsv/{mappingName}")
    public String allMappingsAsCsv(@PathVariable String mappingName) {
        List<String> results = new ArrayList<>();
        results.add("The headers here");
        results.addAll(fixAccountMapRepository.findAll().toStream().map(FixAccountMap::toString).collect(Collectors.toList()));
        return "Dump as string";
    }

    @GetMapping("/findByInternalId/{mappingName}/{id}")
    public FixAccountMap findByInternalId(@PathVariable String mappingName, @PathVariable String internalId) {
        return fixAccountMapRepository.findById(internalId).block();
    }

    @GetMapping("/findByExternalId/{mappingName}/{id}")
    public FixAccountMap findByExternalId(@PathVariable String mappingName, @PathVariable String externalId) {
        return fixAccountMapRepository.findByExternalKey(externalId).blockFirst();
    }

    @PostMapping(value = "/addUpdateMapping/{mappingName}")
    public void addMapping(@PathVariable String mappingName, @RequestBody FixAccountMap fixAccountMap) {
        fixAccountMapRepository.save(fixAccountMap);
    }

    @DeleteMapping(value = "/deleteMapping/{mappingName}/{id}")
    public void deleteMapping(@PathVariable String mappingName, @PathVariable String internalId) {
        fixAccountMapRepository.deleteById(internalId);
    }

}