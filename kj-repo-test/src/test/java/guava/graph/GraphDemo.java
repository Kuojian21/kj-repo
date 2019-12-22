package guava.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;

enum NodeType {
    USER, USER_GROUP, RESOURCE, RESOURCE_GROUP;
}

interface Node<T> {

    NodeType getNodeType();

    long getId();

    T getRaw();
}

interface NodeTransfer<T> {

    Node<T> toNode();
}

/**
 * @author lujunhua 2019/11/14
 */
public class GraphDemo {

    public static void main(String[] args) {
        mutableGraph();
    }

    private static void mutableGraph() {
        MutableGraph<Node<?>> userToResourceGraph = GraphBuilder.directed().build();

        /*
         *               /root
         *               ↙ ↘   ↘
         *             u0  ug0  ug1
         *                 ↙     ↙↘
         *                u1   ug2 ug3
         *                     ↙    ↘
         *                    u2    u3
         *
         *
         *                /root
         *                ↙ ↘   ↘
         *              r0  rg0  rg1
         *                  ↙     ↙↘
         *                 r1   rg2 rg3
         *                      ↙↘    ↘
         *                     r2 r3  r4
         */

        User u0 = new User(0, "u0");
        User u1 = new User(1, "u1");
        User u2 = new User(2, "u2");
        User u3 = new User(3, "u3");
        User u4 = new User(4, "u4");

        UserGroup userGroupRoot = new UserGroup(0, 0, "u_root");
        UserGroup ug0 = new UserGroup(1, 0, "ug0");
        UserGroup ug1 = new UserGroup(2, 0, "ug1");
        UserGroup ug2 = new UserGroup(3, 2, "ug2");
        UserGroup ug3 = new UserGroup(4, 2, "ug3");

        userToResourceGraph.putEdge(u0.toNode(), userGroupRoot.toNode());
        userToResourceGraph.putEdge(ug0.toNode(), userGroupRoot.toNode());
        userToResourceGraph.putEdge(ug1.toNode(), userGroupRoot.toNode());
        userToResourceGraph.putEdge(u1.toNode(), ug0.toNode());
        userToResourceGraph.putEdge(ug2.toNode(), ug1.toNode());
        userToResourceGraph.putEdge(ug3.toNode(), ug1.toNode());
        userToResourceGraph.putEdge(u2.toNode(), ug2.toNode());
        userToResourceGraph.putEdge(u3.toNode(), ug3.toNode());
        /*故意重复一个*/
        userToResourceGraph.putEdge(u3.toNode(), ug3.toNode());

        userToResourceGraph.addNode(u4.toNode()); //孤点

        System.out.println(userToResourceGraph);

        //*****************************************************************************//

        Resource r0 = new Resource(0, "r0");
        Resource r1 = new Resource(1, "r1");
        Resource r2 = new Resource(2, "r2");
        Resource r3 = new Resource(3, "r3");
        Resource r4 = new Resource(4, "r4");

        ResourceGroup resourceRoot = new ResourceGroup(0, 0, "r_root");
        ResourceGroup rg0 = new ResourceGroup(1, 0, "rg0");
        ResourceGroup rg1 = new ResourceGroup(2, 0, "rg1");
        ResourceGroup rg2 = new ResourceGroup(3, 2, "rg2");
        ResourceGroup rg3 = new ResourceGroup(4, 2, "rg3");

        userToResourceGraph.putEdge(resourceRoot.toNode(), r0.toNode());
        userToResourceGraph.putEdge(resourceRoot.toNode(), rg0.toNode());
        userToResourceGraph.putEdge(resourceRoot.toNode(), rg1.toNode());
        userToResourceGraph.putEdge(rg1.toNode(), rg2.toNode());
        userToResourceGraph.putEdge(rg1.toNode(), rg3.toNode());
        userToResourceGraph.putEdge(rg0.toNode(), r1.toNode());
        userToResourceGraph.putEdge(rg2.toNode(), r2.toNode());
        userToResourceGraph.putEdge(rg2.toNode(), r3.toNode());
        userToResourceGraph.putEdge(rg3.toNode(), r4.toNode());

        System.out.println(userToResourceGraph);

        //*****************************************************************************//

        userToResourceGraph.putEdge(ug0.toNode(), resourceRoot.toNode());
        userToResourceGraph.putEdge(u1.toNode(), rg3.toNode());
        userToResourceGraph.putEdge(ug1.toNode(), rg1.toNode());
        userToResourceGraph.putEdge(u2.toNode(), r4.toNode());

        System.out.println(userToResourceGraph);

        Map<NodeType, List<Node>> nodeTypeMap = userToResourceGraph.nodes().stream()
                .collect(Collectors.groupingBy(Node::getNodeType));
        nodeTypeMap.forEach((type, nodes) -> {
            System.out.println("type : " + type);
            System.out.println("nodes : " + nodes);
            System.out.println("=======================================================");
        });

        System.out.println(Graphs.hasCycle(userToResourceGraph));
        System.out.println("=======================================================");

        System.out.println(userToResourceGraph.adjacentNodes(ug0.toNode()));
        System.out.println("=======================================================");

        /*判断是否是直接连接*/
        System.out.println(userToResourceGraph.hasEdgeConnecting(u0.toNode(), r0.toNode()));
        System.out.println("=======================================================");

        System.out.println(Graphs.reachableNodes(userToResourceGraph, u4.toNode()));
        System.out.println("=======================================================");

        Map<NodeType, List<Node>> reachableNodesTypeMap = Graphs
                .reachableNodes(userToResourceGraph, u2.toNode()).stream()
                .collect(Collectors.groupingBy(Node::getNodeType));
        reachableNodesTypeMap.forEach((type, nodes) -> {
            System.out.println("type : " + type);
            System.out.println("nodes : " + nodes);
            System.out.println("=======================================================");
        });

        System.out.println("ug3 parents");
        System.out.println(Graphs.reachableNodes(userToResourceGraph, ug3.toNode()).stream()
                .filter(n -> n.getNodeType() == NodeType.USER_GROUP).collect(Collectors.toSet()));
        System.out.println("=======================================================");

        Set<Node> u2Reach = Streams
                .stream(Traverser.forGraph(userToResourceGraph).depthFirstPreOrder(u2.toNode()))
                .collect(Collectors.toSet());
        System.out.println("u2 reach : " + u2Reach);
        System.out.println("u2 reach : " + Graphs.reachableNodes(userToResourceGraph, u2.toNode()));
        System.out.println("=======================================================");

        Graph<Node<?>> resourceToUserGraph = Graphs.transpose(userToResourceGraph);
        System.out.println(Graphs.reachableNodes(resourceToUserGraph, r2.toNode()));

        System.out.println("=======================================================");
        Multimap<NodePair<Node<?>>, GraphPath<Node<?>>> paths = getPaths(userToResourceGraph,
                u2.toNode());
        Collection<GraphPath<Node<?>>> graphPaths = paths
                .get(new NodePair<>(u2.toNode(), r4.toNode()));
        graphPaths.forEach(graphPath -> System.out.println("u2 to r4 : " + graphPath));
        System.out.println("=======================================================");
        Multimap<NodePair<Node<?>>, GraphPath<Node<?>>> paths1 = getPaths(userToResourceGraph,
                u4.toNode());
        System.out.println(paths1);
    }

    /*参考com.google.common.graph.Graphs.reachableNodes*/
    private static <T> Multimap<NodePair<T>, GraphPath<T>> getPaths(Graph<T> graph, T from) {
        checkNotNull(from);
        Set<T> visitedNodes = new LinkedHashSet<>();
        Queue<T> queuedNodes = new ArrayDeque<>();
        Multimap<NodePair<T>, GraphPath<T>> nodePairGraphPathMultimap = HashMultimap.create();

        visitedNodes.add(from);
        queuedNodes.add(from);
        NodePair<T> topNodePair = new NodePair<>(from, from);
        nodePairGraphPathMultimap.put(topNodePair, new GraphPath<T>().add(topNodePair));

        // breadth-first
        while (!queuedNodes.isEmpty()) {
            T currentNode = queuedNodes.remove();
            NodePair<T> currentNodePair = new NodePair<>(from, currentNode);
            Collection<GraphPath<T>> currentGraphPaths = nodePairGraphPathMultimap
                    .get(currentNodePair);

            for (T successor : graph.successors(currentNode)) {
                /*加入成功表明第一次访问,后续访问将会返回false*/
                if (visitedNodes.add(successor)) {
                    queuedNodes.add(successor);
                }

                currentGraphPaths.forEach(graphPath -> {
                    /*from -> currentNode的基础上再加上currentNode -> successor*/
                    GraphPath<T> fromToSuccessorGraphPath = new GraphPath<T>()
                            .copyOf(graphPath.getEndpointPairSet())
                            .add(new NodePair<>(currentNode, successor));
                    nodePairGraphPathMultimap.put(new NodePair<>(from, successor),
                            fromToSuccessorGraphPath);
                });
            }
        }

        /*GraphPath中去掉from -> from这个路径*/
        nodePairGraphPathMultimap.forEach((nodePair, graphPath) -> graphPath.getEndpointPairSet()
                .remove(EndpointPair.ordered(from, from)));
        return nodePairGraphPathMultimap;
    }
}

abstract class AbstractNode<T> implements Node<T> {

    public String toString() {
        return getRaw().toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNodeType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Node objNode = ((Node) obj);
        return this.getNodeType() == objNode.getNodeType() && this.getId() == objNode.getId();
    }
}

class User implements NodeTransfer<User> {

    private long userId;
    private String userName;

    public User(long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return userName;
    }

    @Override
    public Node<User> toNode() {
        return new UserNode(this);
    }
}

class UserNode extends AbstractNode<User> {

    private User user;

    public UserNode(User user) {
        Preconditions.checkNotNull(user);
        this.user = user;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.USER;
    }

    @Override
    public long getId() {
        return user.getUserId();
    }

    @Override
    public User getRaw() {
        return user;
    }
}

class UserGroup implements NodeTransfer<UserGroup> {

    private long groupId;
    private long parentGroupId;
    private String groupName;

    public UserGroup(long groupId, long parentGroupId, String groupName) {
        this.groupId = groupId;
        this.parentGroupId = parentGroupId;
        this.groupName = groupName;
    }

    public long getParentGroupId() {
        return parentGroupId;
    }

    public long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String toString() {
        return groupName;
    }

    @Override
    public Node<UserGroup> toNode() {
        return new UserGroupNode(this);
    }
}

class UserGroupNode extends AbstractNode<UserGroup> {

    private UserGroup userGroup;

    public UserGroupNode(UserGroup userGroup) {
        Preconditions.checkNotNull(userGroup);
        this.userGroup = userGroup;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.USER_GROUP;
    }

    @Override
    public long getId() {
        return userGroup.getGroupId();
    }

    @Override
    public UserGroup getRaw() {
        return userGroup;
    }
}

class Resource implements NodeTransfer<Resource> {

    private long resourceId;
    private String resourceName;

    public Resource(long resourceId, String resourceName) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    @Override
    public String toString() {
        return resourceName;
    }

    @Override
    public Node<Resource> toNode() {
        return new ResourceNode(this);
    }
}

class ResourceNode extends AbstractNode<Resource> {

    private Resource resource;

    public ResourceNode(Resource resource) {
        Preconditions.checkNotNull(resource);
        this.resource = resource;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RESOURCE;
    }

    @Override
    public long getId() {
        return resource.getResourceId();
    }

    @Override
    public Resource getRaw() {
        return resource;
    }
}

class ResourceGroup implements NodeTransfer<ResourceGroup> {

    private long groupId;
    private long parentGroupId;
    private String groupName;

    public ResourceGroup(long groupId, long parentGroupId, String groupName) {
        this.groupId = groupId;
        this.parentGroupId = parentGroupId;
        this.groupName = groupName;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getParentGroupId() {
        return parentGroupId;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String toString() {
        return groupName;
    }

    @Override
    public Node<ResourceGroup> toNode() {
        return new ResourceGroupNode(this);
    }
}

class ResourceGroupNode extends AbstractNode<ResourceGroup> {

    private ResourceGroup resourceGroup;

    public ResourceGroupNode(ResourceGroup resourceGroup) {
        Preconditions.checkNotNull(resourceGroup);
        this.resourceGroup = resourceGroup;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RESOURCE_GROUP;
    }

    @Override
    public long getId() {
        return resourceGroup.getGroupId();
    }

    @Override
    public ResourceGroup getRaw() {
        return resourceGroup;
    }
}

class NodePair<T> {

    private T from;
    private T to;

    public NodePair(T from, T to) {
        this.from = from;
        this.to = to;
    }

    public T getFrom() {
        return from;
    }

    public T getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodePair<?> nodePair = (NodePair<?>) o;
        return Objects.equals(from, nodePair.from) && Objects.equals(to, nodePair.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}

class GraphPath<T> {

    private Set<EndpointPair<T>> endpointPairSet;

    public GraphPath() {
        endpointPairSet = new LinkedHashSet<>();
    }

    public static <U> Comparator<GraphPath<U>> getComparatorByEndPointPairSize() {
        return Comparator.comparing(gp -> gp.endpointPairSet.size());
    }

    public GraphPath<T> copyOf(Set<EndpointPair<T>> pairs) {
        endpointPairSet.addAll(pairs);
        return this;
    }

    public Set<EndpointPair<T>> getEndpointPairSet() {
        return endpointPairSet;
    }

    public GraphPath<T> add(NodePair<T> nodePair) {
        endpointPairSet.add(EndpointPair.ordered(nodePair.getFrom(), nodePair.getTo()));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GraphPath<?> graphPath = (GraphPath<?>) o;
        return Objects.equals(endpointPairSet, graphPath.endpointPairSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointPairSet);
    }

    @Override
    public String toString() {
        return endpointPairSet.toString();
    }
}
