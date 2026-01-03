package pt.psoft.g1.psoftg1.shared.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PhotoExtendedTest {

    @Test
    void ensurePhotoWithSimpleFilename() {
        final var photo = new Photo(Paths.get("photo.jpg"));
        assertEquals("photo.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithJpgExtension() {
        final var photo = new Photo(Paths.get("image.jpg"));
        assertEquals("image.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithPngExtension() {
        final var photo = new Photo(Paths.get("image.png"));
        assertEquals("image.png", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithGifExtension() {
        final var photo = new Photo(Paths.get("image.gif"));
        assertEquals("image.gif", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithWebpExtension() {
        final var photo = new Photo(Paths.get("image.webp"));
        assertEquals("image.webp", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithUppercaseExtension() {
        final var photo = new Photo(Paths.get("image.JPG"));
        assertEquals("image.JPG", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithComplexFilename() {
        final var photo = new Photo(Paths.get("my-photo-2023.jpg"));
        assertEquals("my-photo-2023.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithNumbers() {
        final var photo = new Photo(Paths.get("photo123456.jpg"));
        assertEquals("photo123456.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithUnderscores() {
        final var photo = new Photo(Paths.get("my_photo_file.jpg"));
        assertEquals("my_photo_file.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithPath() {
        final var photo = new Photo(Paths.get("uploads", "photos", "image.jpg"));
        assertNotNull(photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithAbsolutePath() {
        final var absolutePath = Paths.get("uploads-psoft-g1/photoTest.jpg").toAbsolutePath();
        final var photo = new Photo(absolutePath);
        assertNotNull(photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithSingleCharacterName() {
        final var photo = new Photo(Paths.get("a.jpg"));
        assertEquals("a.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithNoExtension() {
        final var photo = new Photo(Paths.get("photofile"));
        assertEquals("photofile", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithMultipleDots() {
        final var photo = new Photo(Paths.get("my.photo.test.jpg"));
        assertEquals("my.photo.test.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoEquality() {
        final var photo1 = new Photo(Paths.get("photo.jpg"));
        final var photo2 = new Photo(Paths.get("photo.jpg"));
        
        assertEquals(photo1.getPhotoFile(), photo2.getPhotoFile());
    }

    @Test
    void ensurePhotoDifferentFiles() {
        final var photo1 = new Photo(Paths.get("photo1.jpg"));
        final var photo2 = new Photo(Paths.get("photo2.jpg"));
        
        assertNotEquals(photo1.getPhotoFile(), photo2.getPhotoFile());
    }

    @Test
    void ensurePhotoWithHyphens() {
        final var photo = new Photo(Paths.get("my-awesome-photo-2023.jpg"));
        assertEquals("my-awesome-photo-2023.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoThumbnailSize() {
        final var photo = new Photo(Paths.get("thumbnail_small.jpg"));
        assertEquals("thumbnail_small.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithDateFormat() {
        final var photo = new Photo(Paths.get("photo_20231201.jpg"));
        assertEquals("photo_20231201.jpg", photo.getPhotoFile());
    }

    @Test
    void ensurePhotoWithTimestamp() {
        final var photo = new Photo(Paths.get("photo_2023120115300.jpg"));
        assertEquals("photo_2023120115300.jpg", photo.getPhotoFile());
    }
}
