package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.nosql.MongoCustomerRepository;
import at.spengergasse.sj2526pos1javaseed.nosql.model.CustomerDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * dünne rest api für mongo customer docs
 *
 * - nur mongo, kein jpa (issue vorgabe)
 * - ruft nur MongoCustomerRepository auf, keine eigene logik
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/items")
@Tag(name = "Benchmark Items", description = "CRUD on MongoDB customer documents")
public class BenchmarkItemController {

    private final MongoCustomerRepository mongoRepository;

    @Operation(summary = "Get items", description = "Read customer documents from MongoDB")
    @GetMapping
    public List<CustomerDocument> getAll(@RequestParam(defaultValue = "100") int limit) {
        return mongoRepository.findAll(limit);
    }

    @Operation(summary = "Get item", description = "Read one customer document by id")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDocument> getById(@PathVariable long id) {
        return mongoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create item", description = "Insert a customer document into MongoDB")
    @PostMapping
    public ResponseEntity<CustomerDocument> create(@RequestBody CustomerDocument doc) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mongoRepository.insert(doc));
    }

    @Operation(summary = "Update item", description = "Replace a customer document in MongoDB")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDocument> update(@PathVariable long id,
                                                   @RequestBody CustomerDocument doc) {
        return mongoRepository.replace(id, doc)
                ? ResponseEntity.ok(mongoRepository.findById(id).orElseThrow())
                : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Delete item", description = "Delete a customer document from MongoDB")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return mongoRepository.deleteById(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
