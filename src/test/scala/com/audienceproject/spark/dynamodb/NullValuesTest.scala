package com.audienceproject.spark.dynamodb

import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, CreateTableRequest, KeySchemaElement, ProvisionedThroughput}
import com.audienceproject.spark.dynamodb.implicits._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{ArrayType, IntegerType, StringType, StructField, StructType}

class NullValuesTest extends AbstractInMemoryTest {

    test("Insert nested StructType with null values") {
        dynamoDB.createTable(new CreateTableRequest()
            .withTableName("NullTest")
            .withAttributeDefinitions(new AttributeDefinition("name", "S"))
            .withKeySchema(new KeySchemaElement("name", "HASH"))
            .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)))

        val schema = StructType(
            Seq(
                StructField("name", StringType, nullable = false),
                StructField("info", StructType(
                    Seq(
                        StructField("age", IntegerType, nullable = true),
                        StructField("address", StringType, nullable = true)
                    )
                ), nullable = true),
                StructField("numbers", ArrayType(IntegerType, true), nullable = true),
                StructField("colors", ArrayType(StringType, true), nullable = true)
            )
        )

        val rows = spark.sparkContext.parallelize(Seq(
            Row("one", Row(30, "Somewhere"), List(1, 2, 3), List("Blue", "Green")),
            Row("two", null, List(1, 2, 3), List("Blue", "Green")),
            Row("three", Row(null, null), List(1, 2, 3), List("Blue", "Green")),
            Row("four", Row(30, "Somewhere"), List(1, null, 3), List("Blue", null, "Green")),
            Row("five", Row(30, "Somewhere"), List(1, 2, 3, null), List("Blue", "Green", null))
        ))

        val newItemsDs = spark.createDataFrame(rows, schema)

        newItemsDs.write.dynamodb("NullTest")

        val validationDs = spark.read.dynamodb("NullTest")

        validationDs.show(false)
    }

}
