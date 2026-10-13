
package com.examly.springapp;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringappApplication.class)
@AutoConfigureMockMvc
public class SpringappLeaveTests {

    @Autowired
    private MockMvc mockMvc;

    // === API TESTS ===

    @Test
    void SpringBoot_DevelopCoreAPIsAndBusinessLogic_test_Add_Leave() throws Exception {
        String json = """
        {
          "employeeName": "Alice",
          "startDate": "2025-08-10",
          "endDate": "2025-08-12",
          "reason": "Medical",
          "status": "Pending"
        }
        """;

        mockMvc.perform(post("/leaves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void SpringBoot_DevelopCoreAPIsAndBusinessLogic_test_Get_All_Leaves() throws Exception {
        mockMvc.perform(get("/leaves")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // === DIRECTORY CHECKS ===

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_Controller_Directory_Exists() {
        File dir = new File("src/main/java/com/examly/springapp/controller");
        assertTrue(dir.exists() && dir.isDirectory());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_Model_Directory_Exists() {
        File dir = new File("src/main/java/com/examly/springapp/model");
        assertTrue(dir.exists() && dir.isDirectory());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_Repository_Directory_Exists() {
        File dir = new File("src/main/java/com/examly/springapp/repository");
        assertTrue(dir.exists() && dir.isDirectory());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_Service_Directory_Exists() {
        File dir = new File("src/main/java/com/examly/springapp/service");
        assertTrue(dir.exists() && dir.isDirectory());
    }

    // === FILE CHECKS ===

    @Test
    void SpringBoot_DatabaseAndSchemaSetup_test_LeaveModel_File_Exists() {
        File file = new File("src/main/java/com/examly/springapp/model/Leave.java");
        assertTrue(file.exists());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_LeaveController_File_Exists() {
        File file = new File("src/main/java/com/examly/springapp/controller/LeaveController.java");
        assertTrue(file.exists());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_LeaveRepository_File_Exists() {
        File file = new File("src/main/java/com/examly/springapp/repository/LeaveRepository.java");
        assertTrue(file.exists());
    }

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_LeaveService_File_Exists() {
        File file = new File("src/main/java/com/examly/springapp/service/LeaveService.java");
        assertTrue(file.exists());
    }

    // === CLASS CHECKS ===

    @Test
    void SpringBoot_ProjectAnalysisAndUMLDiagram_test_LeaveModel_Class_Exists() {
        checkClassExists("com.examly.springapp.model.Leave");
    }

    // === FIELD CHECKS ===

    @Test
    void SpringBoot_DatabaseAndSchemaSetup_test_Leave_Model_Has_EmployeeName_Field() {
        checkFieldExists("com.examly.springapp.model.Leave", "employeeName");
    }

    @Test
    void SpringBoot_DatabaseAndSchemaSetup_test_Leave_Model_Has_StartDate_Field() {
        checkFieldExists("com.examly.springapp.model.Leave", "startDate");
    }

    @Test
    void SpringBoot_DatabaseAndSchemaSetup_test_Leave_Model_Has_EndDate_Field() {
        checkFieldExists("com.examly.springapp.model.Leave", "endDate");
    }

    // === UTILITY METHODS ===

    private void checkClassExists(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail("Class " + className + " does not exist.");
        }
    }

    private void checkFieldExists(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            clazz.getDeclaredField(fieldName);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            fail("Field " + fieldName + " not found in " + className);
        }
    }
}
