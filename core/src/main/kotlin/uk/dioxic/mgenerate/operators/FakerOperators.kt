package uk.dioxic.mgenerate.operators

import io.github.serpro69.kfaker.Faker

private val faker = Faker()

val noInputOperators = mapOf<String, Operator<*>>(
    "email" to Operator { faker.internet.email() },
    "domain" to Operator { faker.internet.domain() },
    "domainSuffix" to Operator { faker.internet.domainSuffix() },
    "first" to Operator { faker.name.firstName() },
    "last" to Operator { faker.name.lastName() },
    "name" to Operator { faker.name.name() },
    "maritalStatus" to Operator { faker.demographic.maritalStatus() },
    "demonym" to Operator { faker.demographic.demonym() },
)