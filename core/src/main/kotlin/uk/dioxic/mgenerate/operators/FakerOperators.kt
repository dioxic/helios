package uk.dioxic.mgenerate.operators

import io.github.serpro69.kfaker.Faker
import uk.dioxic.mgenerate.annotations.Operator

private val faker = Faker()

val fakerOperators = mapOf<Operator<*>, List<String>>(
    Operator { faker.internet.email() } to listOf("email"),
    Operator { faker.internet.domain() } to listOf("domain"),
    Operator { faker.internet.domainSuffix() } to listOf("domainSuffix"),
    Operator { faker.name.firstName() } to listOf("first", "firstName"),
    Operator { faker.name.lastName() } to listOf("last", "lastName"),
    Operator { faker.name.name() } to listOf("name"),
    Operator { faker.demographic.maritalStatus() } to listOf("maritalStatus"),
    Operator { faker.demographic.demonym() } to listOf("demonym"),
)