package org.example.c4;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.structurizr.Workspace;
import com.structurizr.component.ComponentFinder;
import com.structurizr.component.ComponentFinderBuilder;
import com.structurizr.component.ComponentFinderStrategyBuilder;
import com.structurizr.component.matcher.AnnotationTypeMatcher;
import com.structurizr.component.matcher.NameSuffixTypeMatcher;
import com.structurizr.component.matcher.RegexTypeMatcher;
import com.structurizr.io.json.JsonWriter;
import com.structurizr.model.*;
import com.structurizr.model.Component;
import com.structurizr.view.*;
import org.example.componentDetail.*;


public class AvatarC4ModelGenerator {
    public static void main(String[] args) throws Exception {
        // Create workspace
        Workspace workspace = new Workspace("Avatar C4 Model", "Component analysis for Avatar Connector System");
        Model model = workspace.getModel();
        ViewSet views = workspace.getViews();

        // Define users and external systems
        Person clientUser = model.addPerson("Client User", "Uses the Avatar system to access healthcare data");

        // Define the main software system
        SoftwareSystem avatarSystem = model.addSoftwareSystem("Avatar System", "Connector-based system for healthcare data exchange");
        clientUser.uses(avatarSystem, "Makes data requests through");

        // Define containers within the system
        Container connectorApi = avatarSystem.addContainer("Connector API",
                "Defines the core contract for connectors", "Java/OSGi");

        Container connectorModel = avatarSystem.addContainer("Connector Model",
                "EMF-based data models", "Java/EMF");

        Container connectorImplementations = avatarSystem.addContainer("Connector Implementations",
                "Implementations of the API for different protocols", "Java/OSGi");

        Container connectorInfrastructure = avatarSystem.addContainer("Infrastructure",
                "Supporting components and services", "Java/OSGi");

        // Define relationships between containers
        connectorImplementations.uses(connectorApi, "Implements");
        connectorImplementations.uses(connectorModel, "Uses");
        connectorInfrastructure.uses(connectorApi, "Supports");
        connectorInfrastructure.uses(connectorModel, "Uses");
        clientUser.uses(connectorImplementations, "Sends requests to");


        String basePathModel = "C:\\Users\\khale\\IdeaProjects\\avatar-dataspaces-demo\\de.avatar.connector.model";
        String basePathPorject = "C:\\Users\\khale\\IdeaProjects\\avatar-dataspaces-demo\\";
        File json = new File("src/main/java/org/example/json/componentMapper.json");

        ContainerConfig config = ContainerConfig.loadFromFile(json);
        Map<String, ContainerDetail> allContainers = config.getContainerMap();
        Map<String, ComponentDetail> componentMap = null;
        Map<String, ComponentDetail> componentConnectorMap = null;
        ContainerDetail connectorModelDetail = allContainers.get("connectorModel");
        ContainerDetail connectorImplementationDetail = allContainers.get("connectorImplementations");



        if (connectorModelDetail != null) {
            componentMap = connectorModelDetail.getComponentMap();
            componentConnectorMap = connectorImplementationDetail.getComponentMap();
        }



        createInfraComponents(connectorInfrastructure, connectorModel);
        createApiComponents(connectorApi);

        tryScanningForComponents(connectorImplementations, connectorModel, basePathModel, basePathPorject, componentMap , componentConnectorMap);


        // Create container view
        ContainerView containerView = views.createContainerView(avatarSystem, "containers", "Container View");
        containerView.addAllElements();
        containerView.add(clientUser);

        // Create component views for each container
        ComponentView modelComponentView = views.createComponentView(connectorModel,
                "model-components", "Connector Model Components");
        modelComponentView.addAllComponents();

        ComponentView apiComponentView = views.createComponentView(connectorApi,
                "api-components", "Connector API Components");
        apiComponentView.addAllComponents();

        ComponentView implComponentView = views.createComponentView(connectorImplementations,
                "implementation-components", "Connector Implementation Components");
        implComponentView.addAllComponents();

        ComponentView infraComponentView = views.createComponentView(connectorInfrastructure,
                "infrastructure-components", "Infrastructure Components");
        infraComponentView.addAllComponents();

        // Add styles
        Styles styles = views.getConfiguration().getStyles();
        styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);
        styles.addElementStyle(Tags.CONTAINER).background("#438dd5").color("#ffffff");

        styles.addElementStyle("Info").background("#85bb65").color("#ffffff");
        styles.addElementStyle("Request").background("#7560ba").color("#ffffff");
        styles.addElementStyle("Response").background("#e6bd56").color("#000000");
        styles.addElementStyle("Result").background("#b86950").color("#ffffff");
        styles.addElementStyle("Endpoint").background("#60aa9f").color("#ffffff");
        styles.addElementStyle("API").background("#4b79cc").color("#ffffff");
        styles.addElementStyle("Implementation").background("#f5da55").color("#000000");
        styles.addElementStyle("Infrastructure").background("#b86950").color("#ffffff");

        // Export to JSON
        try (Writer writer = new FileWriter("avatar-c4-model.json")) {
            new JsonWriter(true).write(workspace, writer);
            System.out.println("Avatar C4 model exported to avatar-c4-model.json");
        }
    }


    // Method to manually create API components based on documentation
    private static void createApiComponents(Container container) {
        Component avatarConnectorInfo = container.addComponent("AvatarConnectorInfo",
                "Base interface providing metadata about connectors", "Java Interface");
        avatarConnectorInfo.addTags("API");

        Component avatarConnector = container.addComponent("AvatarConnector",
                "Main service interface for connector implementations", "Java Interface");
        avatarConnector.addTags("API");

        avatarConnector.uses(avatarConnectorInfo, "extends");
    }


    // Method to manually create infrastructure components
    private static void createInfraComponents(Container container, Container model) {
        Component whiteboard = container.addComponent("ConnectorWhiteboard",
                "Implements the OSGi whiteboard pattern for tracking connector services", "Java/OSGi");
        whiteboard.addTags("Infrastructure");

        Component serializer = container.addComponent("EcoreSerializer",
                "Performs serialization/deserialization of EMF objects", "Java/OSGi");
        serializer.addTags("Infrastructure");

    }


    private static void tryScanningForComponents(Container container1, Container container2, String basePath, String basePath2, Map<String, ComponentDetail> componentMap , Map<String, ComponentDetail> componentConnectorMap) {
        File path = new File(basePath);
        File path2 = new File(basePath2);
        if (!path.exists()) {
            System.out.println("Warning: Path " + basePath + " doesn't exist. Skipping component scanning.");
            return;
        }
        try {
            System.out.println("Attempting to scan for components in: " + basePath);

            tryScanningConnectorByOsgiComponentAnnotation(container1, path2,componentConnectorMap);
            tryScanningByOSGiFindAllModel(container2, path, componentMap);


        } catch (Exception e) {
            System.out.println("Component scanning failed: " + e.getMessage());
        }
    }



    private static void tryScanningConnectorByOsgiComponentAnnotation(Container container, File path ,  Map<String, ComponentDetail> componentMap) {
        try {
            ComponentFinder finder = new ComponentFinderBuilder()
                    .forContainer(container)
                    .fromClasses(path)
                    .withStrategy(
                            new ComponentFinderStrategyBuilder()
                                    .matchedBy(new NameSuffixTypeMatcher("ConnectorImpl"))
                                    .withTechnology("OSGi Connector")
                                    .forEach(component -> {
                                        System.out.println("Found OSGi component: " + component.getName());
                                    })
                                    .build()
                    )
                    .build();
            finder.run();
            assignRealtionFromJson(container, componentMap);

            System.out.println("Successfully found OSGi components");
        } catch (Exception e) {
            System.out.println("No OSGi components found – " + e.getMessage());
        }

    }


    /**
     * @param container    the OSGi container
     * @param path         directory or JAR of compiled classes
     * @param componentMap pre‐loaded Map<String, ComponentDetail>
     */
    public static void tryScanningByOSGiFindAllModel(
            Container container,
            File path,
            Map<String, ComponentDetail> componentMap
    ) {
        try {
            ComponentFinder finder = new ComponentFinderBuilder()
                    .forContainer(container)
                    .fromClasses(path)
                    .withStrategy(
                            new ComponentFinderStrategyBuilder()
                                    .matchedBy(
                                            new AnnotationTypeMatcher(
                                                    "org.osgi.annotation.versioning.ProviderType"
                                            )
                                    )
                                    .withTechnology("OSGi Component")
                                    .forEach(component -> {
                                        System.out.println("Found OSGi component: " + component.getName());
                                    })
                                    .build()
                    )
                    .build();
            finder.run();
            assignRealtionFromJson(container, componentMap);
            System.out.println("Successfully found OSGi components");
        } catch (Exception e) {
            System.out.println("No OSGi components found – " + e.getMessage());
        }
    }

    public static void assignRealtionFromJson(Container container, Map<String, ComponentDetail> componentMap) {
        for (Component component : container.getComponents()) {
            for (Map.Entry<String, ComponentDetail> entry : componentMap.entrySet()) {
                String keySubstring = entry.getKey();
                ComponentDetail detail = entry.getValue();

                if (component.getName().contains(keySubstring)) {
                    component.setTechnology(detail.getTechnology());
                    component.addTags(detail.getTags());
                    component.setDescription(detail.getDescription());

                    List<Relations> relations = detail.getRelations();
                    System.out.println(keySubstring + " " + relations);
                    if (relations != null && !relations.isEmpty()) {
                        for (Relations relation : relations) {
                            System.out.println("Relation: " + relation.getType() + " to " + relation.getTarget());
                            Component targetComponent = container.getComponentWithName(relation.getTarget());
                            if (targetComponent != null) {
                                component.uses(targetComponent, relation.getType());
                                System.out.println("Added relation from " + component.getName() + " to " + targetComponent.getName() + " of type " + relation.getType());
                            } else {
                                System.out.println("Target component " + relation.getTarget() + " not found for relation in " + component.getName());
                            }
                        }
                    }
                }
            }
        }
    }

}