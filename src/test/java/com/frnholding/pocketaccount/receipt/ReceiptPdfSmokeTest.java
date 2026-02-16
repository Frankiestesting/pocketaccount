package com.frnholding.pocketaccount.receipt;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptPdfSmokeTest {

    @Test
    void printsReceiptFilesContent() throws Exception {
        Path receiptsDir = Path.of("testdata", "receipts");
        assertThat(Files.isDirectory(receiptsDir))
                .as("Receipts folder should exist at %s", receiptsDir.toAbsolutePath())
                .isTrue();

        List<Path> files = Files.list(receiptsDir)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        assertThat(files)
                .as("Receipts folder should contain files")
                .isNotEmpty();

        for (Path file : files) {
            String filename = file.getFileName().toString();
            String lower = filename.toLowerCase();
            System.out.println("Receipt file: " + filename);
            System.out.println("------------------------------");

            if (lower.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.toFile())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(document);
                    assertThat(text).isNotBlank();

                    ExtractedReceipt extracted = extractReceipt(text);
                    System.out.println(text);
                    System.out.println();
                    System.out.println("Extracted currency: " + extracted.currency);
                    System.out.println("Extracted amount: " + extracted.amount);
                    System.out.println("Extracted buy date: " + extracted.buyDate);
                    System.out.println("Extracted description: " + extracted.description);

                    assertThat(extracted.currency).isNotBlank();
                    assertThat(extracted.amount).isNotBlank();
                    assertThat(extracted.buyDate).isNotBlank();
                    assertThat(extracted.description).isNotBlank();
                }
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                BufferedImage image = ImageIO.read(file.toFile());
                assertThat(image).as("Image should be readable: %s", filename).isNotNull();
                System.out.printf("Image: %dx%d\n", image.getWidth(), image.getHeight());
            } else {
                System.out.println("Unsupported file type");
            }

            System.out.println();
        }
    }

    private ExtractedReceipt extractReceipt(String text) {
        ExtractedReceipt extracted = new ExtractedReceipt();

        Pattern amountPattern = Pattern.compile("(NOK|EUR|USD)\\s*([0-9]+[\\.,][0-9]{2})");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find()) {
            extracted.currency = amountMatcher.group(1);
            extracted.amount = amountMatcher.group(2).replace(',', '.');
        }

        Pattern datePattern = Pattern.compile("Kjøpsdato:\\s*([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            extracted.buyDate = dateMatcher.group(1);
        }

        extracted.description = extractDescription(text).orElse("");
        return extracted;
    }

    private Optional<String> extractDescription(String text) {
        String[] lines = text.split("\\R");
        List<String> candidates = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.matches("[0-9]+") || trimmed.length() < 3) {
                continue;
            }
            if (trimmed.startsWith("Inngang/seksjon") || trimmed.startsWith("Plass") || trimmed.startsWith("Rad")
                    || trimmed.startsWith("Ordrenummer") || trimmed.startsWith("Kjøpsdato")
                    || trimmed.startsWith("inkl. gebyr")) {
                continue;
            }
            candidates.add(trimmed);
        }

        return candidates.stream().findFirst();
    }

    private static class ExtractedReceipt {
        private String currency = "";
        private String amount = "";
        private String buyDate = "";
        private String description = "";
    }
}
