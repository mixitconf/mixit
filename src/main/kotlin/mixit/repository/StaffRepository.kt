package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.Staff
import mixit.support.getEntityInformation
import org.springframework.core.io.ResourceLoader
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class StaffRepository(val db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory, val resourceLoader: ResourceLoader) :
        SimpleReactiveMongoRepository<Staff, String>(f.getEntityInformation(Staff::class), db) {


    fun initData() {
        deleteAll().block()

        val file = resourceLoader.getResource("classpath:data/staff_mixit.json")

        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var staffs: List<MemberDataDto> = objectMapper.readValue(file.file)

        staffs
                .map { staff -> staff.toStaff() }
                .forEach { staff -> save(staff).block() }
    }
}
