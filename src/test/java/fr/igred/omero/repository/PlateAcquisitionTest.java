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

package fr.igred.omero.repository;


import fr.igred.omero.UserTest;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class PlateAcquisitionTest extends UserTest {


    @Test
    public void testAddTagToPlateAcquisition() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);

        TagAnnotationWrapper tag = new TagAnnotationWrapper(client, "Plate acq. tag", "tag attached to a plate acq.");
        acq.addTag(client, tag);
        List<TagAnnotationWrapper> tags = acq.getTags(client);
        client.delete(tag);
        List<TagAnnotationWrapper> checkTags = acq.getTags(client);

        assertEquals(1, tags.size());
        assertEquals(0, checkTags.size());
    }


    @Test
    public void testSetName() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);

        String name  = acq.getName();
        String name2 = "New name";
        acq.setName(name2);
        acq.saveAndUpdate(client);
        assertEquals(name2, client.getPlate(PLATE1.id).getPlateAcquisitions().get(0).getName());

        acq.setName(name);
        acq.saveAndUpdate(client);
        assertEquals(name, client.getPlate(PLATE1.id).getPlateAcquisitions().get(0).getName());
    }


    @Test
    public void testSetDescription() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);

        String name  = acq.getDescription();
        String name2 = "New description";
        acq.setDescription(name2);
        acq.saveAndUpdate(client);
        assertEquals(name2, client.getPlate(PLATE1.id).getPlateAcquisitions().get(0).getDescription());

        acq.setDescription(name);
        acq.saveAndUpdate(client);
        assertEquals(name, client.getPlate(PLATE1.id).getPlateAcquisitions().get(0).getDescription());
    }


    @Test
    public void testGetLabel() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);
        assertEquals(acq.getName(), acq.getLabel());
    }


    @Test
    public void testGetRefPlateId() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);
        assertEquals(-1, acq.getRefPlateId());
    }


    @Test
    public void testGetStartTime() throws Exception {
        final long time = 1146766431000L;

        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);
        assertEquals(time, acq.getStartTime().getTime());
    }


    @Test
    public void testGetEndTime() throws Exception {
        final long time = 1146766431000L;

        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);
        assertEquals(time, acq.getEndTime().getTime());
    }


    @Test
    public void testGetMaximumFieldCount() throws Exception {
        PlateWrapper plate = client.getPlate(PLATE1.id);

        PlateAcquisitionWrapper acq = plate.getPlateAcquisitions().get(0);
        assertEquals(-1, acq.getMaximumFieldCount());
    }

}