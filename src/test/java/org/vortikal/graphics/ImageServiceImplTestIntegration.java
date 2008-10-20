package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;

public class ImageServiceImplTestIntegration extends TestCase {
    
    private final String pngImage = "originalPNGImage.png";
    private final String jpgImage = "originalJPGImage.jpg";
    private final String gifImage = "originalGIFImage.gif";
    private final String bmpImage = "originalBMPImage.bmp";
    private final String notScaledImage = "originalNotScaledImage.png";
    
    private ImageServiceImpl imageService;

    
    protected void setUp() throws Exception {
        super.setUp();
        imageService = new ImageServiceImpl();
    }
    
    public void testScaleImagePNG() throws IOException {
        assertProperResize(pngImage, "300", "");
    }
    
    public void testScaleImageJPG() throws IOException {
        assertProperResize(jpgImage, "300", null);
    }
    
    public void testScaleImageGIF() throws IOException {
        assertProperResize(gifImage, "  ", "300");
    }
    
    public void testScaleImageTIF() throws IOException {
        assertProperResize(bmpImage, "250", null);
    }
    
    public void testDontScaleImage() throws IOException {
        assertProperResize(notScaledImage, "", "");
        assertProperResize(notScaledImage, null, "");
        assertProperResize(notScaledImage, "", null);
        assertProperResize(notScaledImage, null, null);
    }
    
    public void testFailUknownPathFormat() {
    	ScaledImage scaledImage = null;
    	String[] invalidPaths = {"", "http://www.uio.no/image.pn", "http://www.uio.no", "www.uio.no/image.jg"};
    	for (int i = 0; i < invalidPaths.length; i++) {
    		try {
    			scaledImage = imageService.scaleImage(invalidPaths[i], "200", "200");
    			fail();
    		} catch (IOException e) {
    			assertNull("Should be null", scaledImage);
    			assertTrue("Wrong exception", e.getMessage().contains("uknown pathformat"));
    		}
		}
    }
    
    public void testScaleByPath() throws IOException {
    	String scaledWidth = "100";
    	ScaledImage scaledImage = imageService.scaleImage(
    			"http://somstudenter.files.wordpress.com/2007/05/uio-logo.jpg", scaledWidth, "");
    	assertNotNull("No image was fetched", scaledImage);
    	assertEquals("Wrong format", "jpg", scaledImage.getOriginalFormat());
    	assertEquals("Wrong width after resizing", scaledWidth, String.valueOf(scaledImage.getImage().getWidth()));
    }
    
    public void testGetImageBytes() throws IOException {
    	String scaledWidth = "100";
    	BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(pngImage));
    	ScaledImage scaledImage = imageService.scaleImage(originalImage, "png", scaledWidth, null);
    	assertNotNull("No image returned", scaledImage);
    	byte[] imageBytes = scaledImage.getImageBytes("png");
    	assertTrue("No imagebytes returned", imageBytes != null && imageBytes.length > 0);
    	ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
    	BufferedImage imageFromBytes = ImageIO.read(in);
    	assertNotNull("Could not recreate image from bytes", imageFromBytes);
    	assertEquals("Wrong width", scaledWidth, String.valueOf(imageFromBytes.getWidth()));
    }
    
    private void assertProperResize(String imageName, String width, String height) throws IOException {
        BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(imageName));
        String format = imageName.substring(imageName.lastIndexOf(".") + 1);
        ScaledImage scaledImage = imageService.scaleImage(originalImage, format, width, height);
        assertNotNull("No image returned", scaledImage);
        assertEquals("Wrong format", format, scaledImage.getOriginalFormat());
        
        if (StringUtils.isNotBlank(width)) {
        	String scaledWidth = String.valueOf(scaledImage.getImage().getWidth());
            assertEquals("Scaling did not return widht as expected", width, scaledWidth);
        } else if (StringUtils.isNotBlank(height)) {
        	String scaledHeight= String.valueOf(scaledImage.getImage().getHeight());
            assertEquals("Scaling did not return height as expected", height, scaledHeight);
        } else {
        	String originalWidth = String.valueOf(originalImage.getWidth());
        	String scaledWidth = String.valueOf(scaledImage.getImage().getWidth());
        	String originalHeight= String.valueOf(originalImage.getHeight());
        	String scaledHeight= String.valueOf(scaledImage.getImage().getHeight());
        	assertEquals("Scaling did not return widht as expected", scaledWidth, originalWidth);
        	assertEquals("Scaling did not return height as expected", scaledHeight, originalHeight);
        }
        
        // Use this method to print the scaled image for review
        printImage(imageName, scaledImage, null); // print it in it's original format
        if (!StringUtils.equalsIgnoreCase(format, "png")) { // print in in png if any other format
        	printImage(imageName, scaledImage, "png");
        }
    }

    /**
     * Print the scaled image to the tests build folder (target/test-classes/org/vortikal/graphics)
     * Rename it from "original*" to "scaled*"
     * If 'desiredFormat' is set, print image to that format
     */
    private void printImage(String testImageName, ScaledImage scaledImage, String desiredFormat) throws IOException {
        URL url = this.getClass().getResource(testImageName);
        String path = url.toString();
        path = path.substring(path.indexOf("/")).replace("original", "scaled");
        String format = scaledImage.getOriginalFormat();
        if (StringUtils.isNotBlank(desiredFormat)) {
        	format = desiredFormat;
        	path = path.substring(0, path.lastIndexOf(".") + 1) + desiredFormat;
        }
        ImageIO.write(scaledImage.getImage(), format, new File(path));
    }

}
