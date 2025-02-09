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


import fr.igred.omero.AnnotatableWrapper;
import fr.igred.omero.Client;
import fr.igred.omero.GatewayWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ExceptionHandler;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import omero.ServerError;
import omero.gateway.model.DataObject;
import omero.gateway.util.PojoMapper;
import omero.model.Pixels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * Generic class containing a DataObject (or a subclass) object.
 *
 * @param <T> Subclass of {@link DataObject}
 */
public abstract class GenericRepositoryObjectWrapper<T extends DataObject> extends AnnotatableWrapper<T> {

    /**
     * Constructor of the class GenericRepositoryObjectWrapper.
     *
     * @param o The DataObject to wrap in the GenericRepositoryObjectWrapper.
     */
    protected GenericRepositoryObjectWrapper(T o) {
        super(o);
    }


    /**
     * Imports all images candidates in the paths to the target in OMERO.
     *
     * @param client  The client handling the connection.
     * @param target  The import target.
     * @param threads The number of threads (same value used for filesets and uploads).
     * @param paths   Paths to the image files on the computer.
     *
     * @return If the import did not exit because of an error.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     * @throws IOException      Cannot read file.
     */
    protected static boolean importImages(GatewayWrapper client, DataObject target, int threads, String... paths)
    throws ServiceException, OMEROServerError, IOException {
        boolean success;

        ImportConfig config = new ImportConfig();
        String       type   = PojoMapper.getGraphType(target.getClass());
        config.target.set(type + ":" + target.getId());
        config.username.set(client.getUser().getUserName());
        config.email.set(client.getUser().getEmail());
        config.parallelFileset.set(threads);
        config.parallelUpload.set(threads);

        OMEROMetadataStoreClient store = client.getImportStore();
        try (OMEROWrapper reader = new OMEROWrapper(config)) {
            ExceptionHandler.ofConsumer(store,
                                        s -> s.logVersionInfo(config.getIniVersionNumber()))
                            .rethrow(ServerError.class, OMEROServerError::new,
                                     "Cannot log version information during import.")
                            .rethrow();
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));

            ImportLibrary library = new ImportLibrary(store, reader);
            library.addObserver(new LoggingImportMonitor());

            ErrorHandler handler = new ErrorHandler(config);

            ImportCandidates candidates = new ImportCandidates(reader, paths, handler);
            success = library.importCandidates(config, candidates);
        } finally {
            client.closeImport();
        }

        return success;
    }


    /**
     * Imports one image file to the target in OMERO.
     *
     * @param client The client handling the connection.
     * @param target The import target.
     * @param path   Path to the image file on the computer.
     *
     * @return The list of IDs of the newly imported images.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    protected static List<Long> importImage(GatewayWrapper client, DataObject target, String path)
    throws ServiceException, OMEROServerError {
        ImportConfig config = new ImportConfig();
        String       type   = PojoMapper.getGraphType(target.getClass());
        config.target.set(type + ":" + target.getId());
        config.username.set(client.getUser().getUserName());
        config.email.set(client.getUser().getEmail());

        Collection<Pixels> pixels = new ArrayList<>(1);

        OMEROMetadataStoreClient store = client.getImportStore();
        try (OMEROWrapper reader = new OMEROWrapper(config)) {
            store.logVersionInfo(config.getIniVersionNumber());
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));

            ImportLibrary library = new ImportLibrary(store, reader);
            library.addObserver(new LoggingImportMonitor());

            ErrorHandler handler = new ErrorHandler(config);

            ImportCandidates candidates = new ImportCandidates(reader, new String[]{path}, handler);

            ExecutorService uploadThreadPool = Executors.newFixedThreadPool(config.parallelUpload.get());

            List<ImportContainer> containers = candidates.getContainers();
            if (containers != null) {
                for (int i = 0; i < containers.size(); i++) {
                    ImportContainer container = containers.get(i);
                    container.setTarget(target.asIObject());
                    List<Pixels> imported = library.importImage(container, uploadThreadPool, i);
                    pixels.addAll(imported);
                }
            }
            uploadThreadPool.shutdown();
        } catch (Throwable e) {
            throw new OMEROServerError(e);
        } finally {
            client.closeImport();
        }

        List<Long> ids = new ArrayList<>(pixels.size());
        pixels.forEach(pix -> ids.add(pix.getImage().getId().getValue()));
        return ids.stream().distinct().collect(Collectors.toList());
    }


    /**
     * Gets the object name.
     *
     * @return See above.
     */
    public abstract String getName();


    /**
     * Gets the object description
     *
     * @return See above.
     */
    public abstract String getDescription();


    /**
     * Copies annotation links from some other object to this one.
     * <p>Kept for API compatibility purposes.</p>
     *
     * @param client The client handling the connection.
     * @param object Other repository object to copy annotations from.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
    public void copyAnnotationLinks(Client client, GenericRepositoryObjectWrapper<?> object)
    throws AccessException, ServiceException, ExecutionException {
        super.copyAnnotationLinks(client, object);
    }


    /**
     * Policy to specify how to handle objects when they are replaced.
     */
    public enum ReplacePolicy {
        /** Unlink objects only */
        UNLINK,

        /** Delete all objects */
        DELETE,

        /** Delete orphaned objects */
        DELETE_ORPHANED
    }

}
