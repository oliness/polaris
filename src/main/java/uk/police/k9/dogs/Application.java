package uk.police.k9.dogs;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                title = "Police K9 Dogs API",
                version = "1.0.0",
                description = """
                        RESTful API for the register of dogs serving with a police force.

                        Records are never physically removed: a `DELETE` marks the record as deleted
                        (`deletedAt` is populated) so that the audit history is preserved.""",
                license = @License(name = "Apache 2.0"),
                contact = @Contact(name = "Police K9 Unit")
        )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
