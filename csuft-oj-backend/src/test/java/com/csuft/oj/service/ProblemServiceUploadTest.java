package com.csuft.oj.service;

import com.csuft.oj.entity.Problem;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.TestCaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemServiceUploadTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsOversizedZipEntryBeforeExtraction() throws Exception {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        TestCaseMapper testCaseMapper = mock(TestCaseMapper.class);
        Problem problem = new Problem();
        problem.setId(1L);
        when(problemMapper.selectById(1L)).thenReturn(problem);
        ProblemService service = new ProblemService(
                problemMapper,
                submissionMapper,
                testCaseMapper,
                tempDir.toString(),
                1024 * 1024,
                10,
                4,
                1024,
                100);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cases.zip",
                "application/zip",
                zipWithEntry("1.in", "12345"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.uploadTestCases(1L, file));

        assertEquals("Zip entry is too large: 1.in", exception.getMessage());
    }

    private byte[] zipWithEntry(String name, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
