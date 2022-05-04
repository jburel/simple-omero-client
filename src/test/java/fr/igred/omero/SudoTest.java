/*
 *  Copyright (C) 2020-2022 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.omero;


import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class SudoTest extends BasicTest {


    @Test
    public void testSudoTag() throws Exception {
        Client root = new Client();
        root.connect(HOST, PORT, "root", "omero".toCharArray(), GROUP1.id);

        Client test = root.sudoGetUser(USER1.name);
        assertEquals(USER1.id, test.getId());
        TagAnnotationWrapper tag = new TagAnnotationWrapper(test, "Tag", "This is a tag");

        DatasetWrapper     dataset = test.getDataset(DATASET1.id);
        List<ImageWrapper> images  = dataset.getImages(test);

        for (ImageWrapper image : images) {
            image.addTag(test, tag);
        }

        List<ImageWrapper> tagged = dataset.getImagesTagged(test, tag);

        int differences = 0;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getId() != tagged.get(i).getId())
                differences++;
        }

        test.delete(tag);
        try {
            test.disconnect();
            root.disconnect();
        } catch (Exception ignored) {
        }

        assertNotEquals(0, images.size());
        assertEquals(images.size(), tagged.size());
        assertEquals(0, differences);
    }


    @Test
    public void sudoImport() throws Exception {
        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=256&sizeY=512.fake";

        Client client4 = new Client();
        client4.connect(HOST, PORT, "testUser4", "password4".toCharArray(), 6L);
        assertEquals(5L, client4.getId());

        Client client3 = client4.sudoGetUser("testUser3");
        assertEquals(4L, client3.getId());
        client3.switchGroup(6L);

        File f = new File("." + File.separator + filename);
        if (!f.createNewFile())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");

        DatasetWrapper dataset = new DatasetWrapper("sudoTest", "");
        dataset.saveAndUpdate(client3);

        assertTrue(dataset.canLink());
        dataset.importImages(client3, f.getAbsolutePath());

        if (!f.delete())
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be deleted.");

        List<ImageWrapper> images = dataset.getImages(client3);
        assertEquals(1, images.size());

        client4.delete(images.get(0));
        client4.delete(dataset);

        try {
            client3.disconnect();
            client4.disconnect();
        } catch (Exception ignored) {
        }
    }

}