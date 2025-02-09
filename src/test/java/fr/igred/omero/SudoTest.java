/*
 *  Copyright (C) 2020-2023 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.omero;


import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SudoTest extends BasicTest {


    @Test
    void testSudoDisconnect() throws Exception {
        Client root = new Client();
        root.connect(HOST, PORT, ROOT.name, "omero".toCharArray(), GROUP1.id);

        Client test = root.sudoGetUser(USER1.name);
        assertEquals(USER1.id, test.getId());
        test.disconnect();
        assertTrue(root.isConnected(), "root has been disconnected by sudo context");
        root.disconnect();
        assertNotEquals(root.getGateway(), test.getGateway(), "Gateways should not be the same");
    }


    @Test
    void testSudoTag() throws Exception {
        Client root = new Client();
        root.connect(HOST, PORT, ROOT.name, "omero".toCharArray(), GROUP1.id);

        Client test = root.sudoGetUser(USER1.name);
        assertEquals(USER1.id, test.getId());
        TagAnnotationWrapper tag = new TagAnnotationWrapper(test, "Tag", "This is a tag");

        DatasetWrapper     dataset = test.getDataset(DATASET1.id);
        List<ImageWrapper> images  = dataset.getImages(test);

        for (ImageWrapper image : images) {
            image.link(test, tag);
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
        } catch (RuntimeException ignored) {
        }

        assertNotEquals(0, images.size());
        assertEquals(images.size(), tagged.size());
        assertEquals(0, differences);
    }


    @Test
    void sudoImport() throws Exception {
        String filename = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=256&sizeY=512.fake";

        Client client4 = new Client();
        client4.connect(HOST, PORT, "testUser4", "password4".toCharArray(), 6L);
        assertEquals(5L, client4.getId());

        Client client3 = client4.sudoGetUser("testUser3");
        assertEquals(4L, client3.getId());
        client3.switchGroup(6L);

        File file = createFile(filename);

        DatasetWrapper dataset = new DatasetWrapper("sudoTest", "");
        dataset.saveAndUpdate(client3);

        assertTrue(dataset.canLink());
        dataset.importImages(client3, file.getAbsolutePath());

        removeFile(file);

        List<ImageWrapper> images = dataset.getImages(client3);
        assertEquals(1, images.size());
        assertEquals(client3.getId(), images.get(0).getOwner().getId());
        assertEquals(6L, images.get(0).getGroupId());

        client4.delete(images.get(0));
        client4.delete(dataset);

        try {
            client3.disconnect();
            client4.disconnect();
        } catch (RuntimeException ignored) {
        }
    }

}
