package com.github.bachelorpraktikum.visualisierbar.view.graph;

import com.github.bachelorpraktikum.visualisierbar.model.*;
import com.github.bachelorpraktikum.visualisierbar.model.train.Train;
import com.github.bachelorpraktikum.visualisierbar.view.graph.adapter.CoordinatesAdapter;
import com.github.bachelorpraktikum.visualisierbar.view.graph.elements.Elements;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.Group;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Graph {

    @Nonnull
    private final Context context;
    @Nonnull
    private final CoordinatesAdapter coordinatesAdapter;

    @Nonnull
    private final Group group;

    @Nonnull
    private  Map<Node, GraphShape<Node>> nodes;
    @Nonnull
    private  Map<Edge, GraphShape<Edge>> edges;
    @Nonnull
    private Map<Element, GraphShape<Element>> elements;


    /**
     * Creates a new graph for the given context. The graph is laid out by using the given {@link
     * CoordinatesAdapter}.
     *
     * @param context the context
     * @param coordinatesAdapter the coordinates adapter to translate coordinates from the model to
     * real coordinates
     * @throws NullPointerException if either argument is null
     * @throws IllegalStateException if there is nothing for this context to show
     */
    public Graph(Context context, CoordinatesAdapter coordinatesAdapter) {
        this.context = Objects.requireNonNull(context);
        this.coordinatesAdapter = Objects.requireNonNull(coordinatesAdapter);
        this.nodes = new LinkedHashMap<>(128);
        this.elements = new LinkedHashMap<>(256);
        this.group = new Group();
        this.edges = new LinkedHashMap<>(256);
        Junction.clearSelection();      //TEST//
        for (Edge edge : Edge.in(context).getAll()) {
            GraphShape<Edge> shape = new Rail(edge, coordinatesAdapter);
            edges.put(edge, shape);
            edge.setGraph(this);
            group.getChildren().add(shape.getFullNode());
        }

        for (Node node : Node.in(context).getAll()) {
            GraphShape<Node> shape = new Junction(node, coordinatesAdapter);

            nodes.put(node, shape);
            node.setGraph(this);
            group.getChildren().add(shape.getFullNode());
            //node.getElements().forEach((a)->{a.setGraph(null);});

            for (GraphShape<Element> elementShape : Elements.create(node, coordinatesAdapter)) {

                for (Element element : elementShape.getRepresentedObjects()) {
                    elements.put(element, elementShape);
                    element.setGraph(this);
                }
                group.getChildren().add(elementShape.getFullNode());

            }
        }

    }

    public void scale(double factor) {
        double scale = group.getScaleX() * factor;
        group.setScaleX(scale);
        group.setScaleY(scale);
    }

    public void move(double x, double y) {
        group.setTranslateX(group.getTranslateX() + x);
        group.setTranslateY(group.getTranslateY() + y);
    }

    public Group getGroup() {
        return group;
    }

    public Map<Node, GraphShape<Node>> getNodes() {
        return nodes;
    }

    public Map<Edge, GraphShape<Edge>> getEdges() {
        return edges;
    }

    public Map<Element, GraphShape<Element>> getElements() {
        return elements;
    }

    public CoordinatesAdapter getCoordinatesAdapter() {
        return coordinatesAdapter;
    }

    /**
     * removes a {@link Node} from the Graph, the Context and the Factory mapping.
     * @param node the Node to remove
     */
    public void removeNode(Node node){

        LinkedList<Element> e =new LinkedList<>();
        //LinkedList<Node> n =new LinkedList();
        LinkedList<Edge> ed =new LinkedList<>();
        elements.forEach((a,b)->{
            if (a.getNode()==node){
                e.add(a);
            }
        });

        edges.forEach((a,b)->{
            if(a.getNode1()==node || a.getNode2()==node) {
                ed.add(a);
                a.getOtherNode(node).getEdges().remove(a);

                group.getChildren().remove(b.getFullNode());    //remove edges from graph pane
                //edges.remove(a);
                a.setGraph(null);
            }
        });

        e.forEach(this::removeElement);

        ed.forEach((a)->{
            Edge.in(context).remove(a);
            edges.remove(a);                                    //remove edges from  context, factory and graph
            context.removeObject(a);
        });
        group.getChildren().remove(nodes.get(node).getFullNode());
        node.setGraph(null);
        nodes.remove(node);                                     //remove node from graph, factory context and graph pane
        Node.in(context).remove(node);
        context.removeObject(node);

    }
    /*
        /**
         * removes an {@link Edge} from the Graph as well as the Context and the Factory mapping.
         * @param edge the Edge to remove
         */
    /*
    public void removeEdge (Edge edge){
        edge.getNode1().getEdges().remove(edge);
        edge.getNode2().getEdges().remove(edge);
        edges.remove(edge);
        context.removeObject(edge);
        Edge.in(context).remove(edge);
    }
    */

    /**
     * Connects every Node in the Selection with every other Node part of the Selection.
     * Already existing Edges are not duplicated.
     * New Edges are given a random name and length -1
     * New Edges are added to the Graph, Context and the Factory mapping.
     * @param selection the Selection to fully connect
     */
    public void fullyConnect (HashSet<Junction> selection){
        HashSet<Node> NodeSet = new HashSet<>(128);
        Iterator it = Junction.getSelection().iterator();
        while(it.hasNext()){
            NodeSet.add(((Junction) it.next()).getRepresented());       //turn Junctions into Nodes
        }
        LinkedList<Node> sList = new LinkedList<>();                      //turn into list to get an order
        sList.addAll(NodeSet);
        int l = selection.size();
        boolean existingEdges[][]=new boolean [l][l];   //mark existing edges in this array

        for (int i=0; i<l; i++){
            for (int k=0; k<l ; k++ ){
                //init array
                existingEdges[i][k] = i == k;
            }
        }
        edges.forEach((a,b)->{
            if (NodeSet.contains(a.getNode1()) && NodeSet.contains(a.getNode2())) {
                int i;
                int j;
                for ( i = 0; i < l; i++) {
                    if(a.getNode1()== sList.get(i)) break;
                }
                for ( j = 0; j < l; j++) {
                    if(a.getNode2()== sList.get(j)) break;
                }
                existingEdges[i][j]=true;               //mark the existing edge
                existingEdges[j][i]=true;
            }
        });

        RandomString gen = new RandomString(8, ThreadLocalRandom.current());
        for (int i=0; i<l; i++){
            for (int k=i; k<l ; k++ ){
                if (!existingEdges[i][k]){
                    String name=null;

                    for (int j=0; j<1000; j++) {               //generate missing edges with random names
                        name = gen.nextString();
                        if(!Edge.in(context).NameExists(name)) break;
                    }
                    Edge edge = Edge.in(context).create(name,-1, sList.get(i), sList.get(k));
                    edge.setGraph(this);
                    context.addObject(edge);
                    GraphShape<Edge> shape = new Rail(edge, coordinatesAdapter);
                    edges.put(edge, shape);
                    group.getChildren().add(shape.getFullNode());
                }
            }
        }

    }

    /**
     * Removes all Edges that are associated with this Node.
     * Edges are removed from the Graph, the Context and the Factory mapping.
     * @param node to disconnect
     */
    public void disconnect (Node node){
        LinkedList<Edge> ed = new LinkedList<>();
        edges.forEach((a,b)-> {
            if (a.getNode1() == node || a.getNode2() == node) {
                Node otherNode=a.getOtherNode(node);
                otherNode.getEdges().remove(a);
                ed.add(a);
                group.getChildren().remove(b.getFullNode());    //remove from graph pane
                a.setGraph(null);
            }
        });
        ed.forEach((a)->{               //remove from graph, factory, context and node
            edges.remove(a);
            context.removeObject(a);
            Edge.in(context).remove(a);
        });
        node.getEdges().clear();
    }

    /**
     * Removes an element from the Graph
     * @param element Element to remove
     */
    public void removeElement (Element element){

        if(element.getType()== Element.Type.WeichenPunkt){
            if (element== element.getSwitch().getMainElement()){
                for(Element we : element.getSwitch().getElements()){
                rE(we);
                }
            }
            else
                removeElement(element.getSwitch().getMainElement());
        }
        else
            rE(element);


    }

    private void rE(Element element){
        element.getNode().getElements().remove(element);
        elements.get(element).getRepresentedObjects().remove(element);
        group.getChildren().remove(elements.get(element).getFullNode());        //TODO only remove when no other represented Objects?
        elements.remove(element);
        Element.in(context).remove(element);                      //remove elements from context, factory and graph
        context.removeObject(element);
        element.setGraph(null);
        if(element.getType().isComposite()){
            for( Element CompositeElement : element.getNode().getElements()){
                if (CompositeElement.getType().isComposite()){                       //remove composite Elements to rebuild them with the new element if necessary
                    if(CompositeElement.getGraph()==this){
                        this.getGroup().getChildren().remove(elements.get(CompositeElement).getFullNode());
                        CompositeElement.setGraph(null);
                    }
                }
            }
        }
        for (GraphShape<Element> elementShape : Elements.create(element.getNode(), coordinatesAdapter)) {           //recreate composite Elements
            for (Element Celement : elementShape.getRepresentedObjects()) {
                elements.put(Celement, elementShape);
                Celement.setGraph(this);
            }
            group.getChildren().add(elementShape.getFullNode());
        }
    }

    /**
     * Adds a new Node with the specified name and {@link Coordinates} to the Graph, Context and the Factory mapping
     * @param name the Name
     * @param coordinates   the coordinates
     * @throws IllegalArgumentException    if coordinates are negative or name is taken
     */
    public void addNode (String name, Coordinates coordinates) throws IllegalArgumentException {
        Node newNode =Node.in(context).create(name, coordinates);
        newNode.setGraph(this);
        if(nodes.containsKey(newNode)) return;
        context.addObject(newNode);
        GraphShape<Node> shape = new Junction(newNode, coordinatesAdapter);
        ((Junction) shape).setMoveable(true);

        nodes.put(newNode, shape);
        group.getChildren().add(shape.getFullNode());
    }

    /**
     * Adds a new Node with the specified name and {@link Coordinates} to the Graph, Context and the Factory mapping
     * @param name the Name
     * @param coordinates   the coordinates
     * @param absName the ABS Name
     * @throws IllegalArgumentException    if coordinates are negative or name is taken
     */
    public void addNode (String name, Coordinates coordinates, String absName) throws IllegalArgumentException {
        Node newNode =Node.in(context).create(name, coordinates, absName);
        newNode.setGraph(this);
        if(nodes.containsKey(newNode)) return;
        context.addObject(newNode);
        GraphShape<Node> shape = new Junction(newNode, coordinatesAdapter);
        ((Junction) shape).setMoveable(true);

        nodes.put(newNode, shape);
        group.getChildren().add(shape.getFullNode());
    }


    /**
     * Adds am Element to the Graph
     * @param elementToAdd
     */
    public void addElement(Element elementToAdd){
        //elementtoAdd.getNode().ge

        if(elementToAdd.getType().isComposite()){
            for( Element element : elementToAdd.getNode().getElements()){
                if (element.getType().isComposite()){                       //remove composite Elements to rebuild them with the new element if necessary
                    if(element.getGraph()==this){
                        this.getGroup().getChildren().remove(elements.get(element).getFullNode());
                        element.setGraph(null);
                    }
                }
            }
        }
        context.addObject(elementToAdd);
        for (GraphShape<Element> elementShape : Elements.create(elementToAdd.getNode(), coordinatesAdapter)) {
            for (Element element : elementShape.getRepresentedObjects()) {
                elements.put(element, elementShape);
                element.setGraph(this);
            }
            group.getChildren().add(elementShape.getFullNode());

        }
    }

    @Nonnull
    public Context getContext(){
        return this.context;
    }

    /**
     * Printing the nodes to ABS-Code to the console
     * If there are some negative nodes this function translate the
     * whole graph to a positive graph
     * @return a String of all Nodes in ABS-Code-Format
     */
    @Nonnull
    public String printToAbs()
         {String response = "";

          // Generating ABS-Code for all nodes
          for(Map.Entry<Node, GraphShape<Node>> entry : nodes.entrySet())
             {response = response.concat(entry.getKey().toABS());}

          response = response.concat("\n");

          // Generating ABS-Code for all edges
          for(Map.Entry<Edge, GraphShape<Edge>> entry : edges.entrySet())
             {response = response.concat(entry.getKey().toABS());}

          response = response.concat("\n");

          // Generating ABS-Code for all trackelements
          for(Map.Entry<Element, GraphShape<Element>> entry : elements.entrySet())
             {response = response.concat(entry.getKey().toABS());}


          // Print it!
          System.out.println("----- ABS start -----");
          System.out.println(response);
          System.out.println("----- ABS end -----");

          return response;
         }
}
