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

package fr.igred.omero.repository;


import fr.igred.omero.Client;
import fr.igred.omero.GenericObjectWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import ome.model.units.BigResult;
import omero.gateway.model.WellSampleData;
import omero.model.Length;
import omero.model.enums.UnitsLength;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Class containing a WellSampleData object.
 * <p> Wraps function calls to the WellSampleData contained.
 */
public class WellSampleWrapper extends GenericObjectWrapper<WellSampleData> {


    /**
     * Constructor of the class WellSampleWrapper.
     *
     * @param wellSample The WellSampleData to wrap in the WellSampleWrapper.
     */
    public WellSampleWrapper(WellSampleData wellSample) {
        super(wellSample);
    }


    /**
     * @return See above.
     *
     * @deprecated Returns the WellSampleData contained. Use {@link #asDataObject()} instead.
     */
    @Deprecated
    public WellSampleData asWellSampleData() {
        return data;
    }


    /**
     * Returns the screens containing the parent Well.
     *
     * @param client The client handling the connection.
     *
     * @return See above.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError   Server error.
     */
    public List<ScreenWrapper> getScreens(Client client)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError {
        return getWell(client).getScreens(client);
    }


    /**
     * Returns the plates containing the parent Well.
     *
     * @param client The client handling the connection.
     *
     * @return See above.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<PlateWrapper> getPlates(Client client) throws ServiceException, AccessException, ExecutionException {
        return Collections.singletonList(getWell(client).getPlate());
    }


    /**
     * Returns the plate acquisitions linked to the parent Well.
     *
     * @param client The client handling the connection.
     *
     * @return See above.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<PlateAcquisitionWrapper> getPlateAcquisitions(Client client)
    throws ServiceException, AccessException, ExecutionException {
        return getWell(client).getPlateAcquisitions(client);
    }


    /**
     * Retrieves the well containing this well sample
     *
     * @param client The client handling the connection.
     *
     * @return See above
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public WellWrapper getWell(Client client) throws AccessException, ServiceException, ExecutionException {
        return client.getWell(asDataObject().asWellSample().getWell().getId().getValue());
    }


    /**
     * Returns the image related to that sample if any.
     *
     * @return See above.
     */
    public ImageWrapper getImage() {
        return new ImageWrapper(data.getImage());
    }


    /**
     * Sets the image linked to this well sample.
     *
     * @param image The image to set.
     */
    public void setImage(ImageWrapper image) {
        data.setImage(image.asDataObject());
    }


    /**
     * Returns the position X.
     *
     * @param unit The unit (can be null, in which case no conversion will be performed)
     *
     * @return See above.
     *
     * @throws BigResult If an arithmetic under-/overflow occurred
     */
    public Length getPositionX(UnitsLength unit) throws BigResult {
        return data.getPositionX(unit);
    }


    /**
     * Returns the position Y.
     *
     * @param unit The unit (can be null, in which case no conversion will be performed)
     *
     * @return See above.
     *
     * @throws BigResult If an arithmetic under-/overflow occurred
     */
    public Length getPositionY(UnitsLength unit) throws BigResult {
        return data.getPositionY(unit);
    }


    /**
     * Returns the time at which the field was acquired.
     *
     * @return See above.
     */
    public long getStartTime() {
        return data.getStartTime();
    }

}
