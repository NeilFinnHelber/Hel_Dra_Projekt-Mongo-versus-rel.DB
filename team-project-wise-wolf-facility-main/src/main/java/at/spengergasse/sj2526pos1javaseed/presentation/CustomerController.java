package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateCustomerCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Customer;
import at.spengergasse.sj2526pos1javaseed.Ids.UserId;
import at.spengergasse.sj2526pos1javaseed.dto.CustomerDTO;
import at.spengergasse.sj2526pos1javaseed.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.NoSuchElementException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1.0.0/customers")
@Validated
@Tag(name = "Customer", description = "Endpoints for managing customers")
public class CustomerController {

    private final CustomerService customerService;
    private final PagedResourcesAssembler<CustomerDTO> pagedResourcesAssembler;

    @Operation(summary = "Create customer", description = "Create customer operation")
    @PostMapping
    public ResponseEntity<EntityModel<CustomerDTO>> createCustomer(@Valid @RequestBody CreateCustomerCommand cmd) {
        Customer customer = customerService.create(cmd);
        CustomerDTO dto = CustomerDTO.from(customer);
        EntityModel<CustomerDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(CustomerController.class).getCustomer(dto.id())).withSelfRel(),
                linkTo(methodOn(CustomerController.class).getAllCustomers(null)).withRel("customers")
        );
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Get customer", description = "Get customer operation")
    @GetMapping("/{customerId}")
    public ResponseEntity<EntityModel<CustomerDTO>> getCustomer(@PathVariable Long customerId) {
        try {
            Customer customer = customerService.getById(new UserId(customerId));
            CustomerDTO dto = CustomerDTO.from(customer);
            EntityModel<CustomerDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(CustomerController.class).getCustomer(customerId)).withSelfRel(),
                    linkTo(methodOn(CustomerController.class).getAllCustomers(null)).withRel("customers")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all customers", description = "Get all customers operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<CustomerDTO>>> getAllCustomers(Pageable pageable) {
        Page<CustomerDTO> page = customerService.getAll(pageable).map(CustomerDTO::from);
        PagedModel<EntityModel<CustomerDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(CustomerController.class).getCustomer(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Update customer", description = "Update customer operation")
    @PatchMapping("/{customerId}")
    public ResponseEntity<EntityModel<CustomerDTO>> updateCustomer(@PathVariable Long customerId,
                                                                   @RequestBody CreateCustomerCommand cmd) {
        try {
            Customer customer = customerService.getById(new UserId(customerId));
            customer.setFirstName(cmd.firstName());
            customer.setLastName(cmd.lastName());
            if (cmd.email() != null) customer.setEmail(cmd.email());
            customerService.updateCustomer(customer);
            CustomerDTO dto = CustomerDTO.from(customer);
            EntityModel<CustomerDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(CustomerController.class).getCustomer(customerId)).withSelfRel()
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete customer", description = "Delete customer operation")
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(new UserId(customerId));
        return ResponseEntity.noContent().build();
    }
}
