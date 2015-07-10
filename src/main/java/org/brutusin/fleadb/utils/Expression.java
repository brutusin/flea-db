/*
 * Copyright 2015 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.fleadb.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.json.spi.JsonSchema;
import org.brutusin.commons.json.util.JsonNodeVisitor;
import org.brutusin.commons.utils.Miscellaneous;
import static org.brutusin.commons.utils.Miscellaneous.countMatches;

/**
 * A compiled and validated expression.
 * <br/><br/>Example:
 * <br/><pre><code>
 *     Expression exp = Expression.compile("$.map[\"akey\"].array[*]");
 *     System.out.println(exp.getTokens());
 * </code></pre> generates the following output:
 * <pre><code>
 *     [$, map, ["akey"], array, [*]]
 * </code></pre>
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Expression {

    private final String expression;
    private final Queue<String> elements;
    private final boolean multivalued;// depending on the ocurrences of [*] or [#] in expression

    private Expression(Queue<String> elements, String expression) {
        if (elements == null) {
            throw new IllegalArgumentException("Elements can not be null");
        }
        this.elements = elements;
        this.expression = expression;
        this.multivalued = countMatches(expression, "\\[[#\\*]\\]") > 0;
    }

    public Queue<String> getTokens() {
        return elements;
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    /**
     * Compiles the expression.
     *
     * @param exp
     * @return
     */
    public static Expression compile(String exp) {
        if (Miscellaneous.isEmpty(exp) || exp.equals(".")) {
            exp = "$";
        }
        Queue<String> queue = new LinkedList<String>();
        List<String> tokens = parseTokens(exp);
        boolean isInBracket = false;
        int numInBracket = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("[")) {
                if (isInBracket) {
                    throw new IllegalArgumentException("Error parsing expression '" + exp + "': Nested [ found");
                }
                isInBracket = true;
                numInBracket = 0;
                sb.append(token);
            } else if (token.equals("]")) {
                if (!isInBracket) {
                    throw new IllegalArgumentException("Error parsing expression '" + exp + "': Unbalanced ] found");
                }
                isInBracket = false;
                sb.append(token);
                queue.add(sb.toString());
                sb = new StringBuilder();
            } else {
                if (isInBracket) {
                    if (numInBracket > 0) {
                        throw new IllegalArgumentException("Error parsing expression '" + exp + "': Multiple tokens found inside a bracket");
                    }
                    sb.append(token);
                    numInBracket++;
                } else {
                    queue.add(token);
                }
            }
            if (i == tokens.size() - 1) {
                if (isInBracket) {
                    throw new IllegalArgumentException("Error parsing expression '" + exp + "': Unbalanced [ found");
                }
            }
        }
        return new Expression(queue, exp);
    }

    /**
     * First step tokenization.
     * <br/><br/>Example, for the following input:
     * <br/><pre><code>
     *      $.map["akey"].array[*]
     * </code></pre> returns:
     * <pre><code>
     *      $
     *      map
     *      [
     *      "akey"
     *      ]
     *      array
     *      [
     *      *
     *      ]
     * </code></pre>
     *
     * @param exp
     * @return
     */
    private static List<String> parseTokens(String exp) {
        if (exp == null) {
            return null;
        }
        List<String> tokens = new ArrayList<String>();
        Character commentChar = null;
        int start = 0;
        for (int i = 0; i < exp.length(); i++) {
            if (exp.charAt(i) == '"') {
                if (commentChar == null) {
                    commentChar = '"';
                } else if (commentChar == '"') {
                    commentChar = null;
                    tokens.add(exp.substring(start, i + 1).trim());
                    start = i + 1;
                }
            } else if (exp.charAt(i) == '\'') {
                if (commentChar == null) {
                    commentChar = '\'';
                } else if (commentChar == '\'') {
                    commentChar = null;
                    tokens.add(exp.substring(start, i + 1).trim());
                    start = i + 1;
                }
            } else if (exp.charAt(i) == '[') {
                if (commentChar == null) {
                    if (start != i) {
                        tokens.add(exp.substring(start, i).trim());
                    }
                    tokens.add("[");
                    start = i + 1;
                }
            } else if (exp.charAt(i) == ']') {
                if (commentChar == null) {
                    if (start != i) {
                        tokens.add(exp.substring(start, i).trim());
                    }
                    tokens.add("]");
                    start = i + 1;
                }
            } else if (exp.charAt(i) == '.') {
                if (commentChar == null) {
                    if (start != i) {
                        tokens.add(exp.substring(start, i).trim());
                    }
                    start = i + 1;
                }
            } else if (i == exp.length() - 1) {
                tokens.add(exp.substring(start, i + 1).trim());
            }
        }
        return tokens;
    }

    /**
     *
     * @param node
     * @param expression
     * @return
     */
    public JsonNode projectNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        final List<JsonNode> nodes = new ArrayList();
        JsonNodeVisitor visitor = new JsonNodeVisitor() {
            public void visit(String name, JsonNode node) {
                if (expression.equals(name)) {
                    nodes.add(node);
                }
            }
        };
        visitNode("", new LinkedList(this.elements), node, visitor);

        if (nodes.size() > 1) {
            return new JsonArrayNode(nodes);
        }
        if (!multivalued) {
            if (nodes.size() > 1) {
                throw new AssertionError();
            } else if (nodes.size() == 1) {
                return nodes.get(0);
            } else {
                return null;
            }
        } else {
            return new JsonArrayNode(nodes);
        }
    }

    /**
     *
     * @param name
     * @param nameTokens
     * @param jsonNode
     * @param visitor
     */
    private static void visitNode(String name, final Queue<String> nameTokens, final JsonNode jsonNode, final JsonNodeVisitor visitor) {
        if (jsonNode == null) {
            return;
        }
        JsonNode.Type nodeType = jsonNode.getNodeType();

        if (nodeType == JsonNode.Type.NULL) {
            return;
        }
        String currentToken = nameTokens.poll();
        if (currentToken != null && currentToken.equals("$")) {
            name = "$";
            currentToken = nameTokens.poll();
        }
        if (currentToken == null) {
            visitor.visit(name, jsonNode);

        } else if (nodeType == JsonNode.Type.ARRAY) {

            if (!currentToken.startsWith("[")) {
                throw new IllegalArgumentException("Node '" + name + "' is of type array");
            }

            String element = currentToken.substring(1, currentToken.length() - 1);
            if (element.equals("#")) {
                for (int i = 0; i < jsonNode.getSize(); i++) {
                    JsonNode child = jsonNode.get(i);
                    visitNode(name + currentToken, new LinkedList<String>(nameTokens), child, visitor);
                    visitNode(name + "[" + i + "]", new LinkedList<String>(nameTokens), child, visitor);
                }
            } else if (element.equals("$")) {
                JsonNode child = jsonNode.get(jsonNode.getSize() - 1);
                name = name + currentToken;
                visitNode(name, new LinkedList<String>(nameTokens), child, visitor);
            } else {
                try {
                    Integer index = Integer.valueOf(element);
                    if (index < 0) {
                        throw new IllegalArgumentException("Element '" + element + "' of node '" + name + "' is lower than zero");
                    }
                    JsonNode child = jsonNode.get(index);
                    name = name + currentToken;
                    visitNode(name, new LinkedList<String>(nameTokens), child, visitor);

                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Element '" + element + "' of node '" + name + "' is not an integer, or the '$' last element symbol,  or the wilcard symbol '#'");
                }
            }
        } else if (nodeType == JsonNode.Type.OBJECT) {
            if (currentToken.equals("[*]")) {
                Iterator<String> fieldNames = jsonNode.getProperties();
                while (fieldNames.hasNext()) {
                    String childName = fieldNames.next();
                    JsonNode child = jsonNode.get(childName);
                    visitNode(name + currentToken, new LinkedList<String>(nameTokens), child, visitor);
                    visitNode(name + "[\"" + childName + "\"]", new LinkedList<String>(nameTokens), child, visitor);
                }
            } else {
                JsonNode child;
                if (currentToken.startsWith("[")) {
                    String element = currentToken.substring(1, currentToken.length() - 1);
                    if (element.startsWith("\"") || element.startsWith("'")) {
                        element = element.substring(1, element.length() - 1);
                    } else {
                        throw new IllegalArgumentException("Element '" + element + "' of node '" + name + "' must be a string expression or wilcard '*'");
                    }
                    name = name + currentToken;
                    child = jsonNode.get(element);
                } else {
                    if (name.length() > 0) {
                        name = name + "." + currentToken;
                    } else {
                        name = currentToken;
                    }
                    child = jsonNode.get(currentToken);
                }
                visitNode(name, nameTokens, child, visitor);
            }
        } else if (nodeType == JsonNode.Type.BOOLEAN
                || nodeType == JsonNode.Type.NUMBER
                || nodeType == JsonNode.Type.STRING) {
            throw new IllegalArgumentException("Node is leaf but still are tokens remaining: " + currentToken);
        } else {
            throw new UnsupportedOperationException("Node type '" + nodeType + "' not supported for index field '" + name + "'");
        }
    }

    /**
     *
     * @param schema
     * @return
     */
    public JsonSchema projectSchema(JsonSchema schema) {
        JsonSchema elementSchema = getSubSchema("", new LinkedList(this.elements), schema);
        if (multivalued) {
            try {
                StringBuilder sb = new StringBuilder(
                        "{\"type\":\"array\",\"items\":");
                sb.append(elementSchema.toString());
                sb.append("}");
                return JsonCodec.getInstance().parseSchema(sb.toString());
            } catch (ParseException ex) {
                throw new AssertionError();
            }
        } else {
            return elementSchema;
        }
    }

    private static JsonSchema getSubSchema(String name, Queue<String> tokens, JsonSchema schema) {

        if (tokens == null || tokens.size() == 0) {
            return schema;
        }
        JsonNode.Type type = schema.getSchemaType();
        String currentToken = tokens.poll();
        if (currentToken != null && currentToken.equals("$")) {
            name = "$";
            currentToken = tokens.poll();
        }
        if (currentToken == null) {
            return schema;
        } else if (schema == null) {
            throw new IllegalArgumentException("No supported node '" + name + "'");
        } else if (type == JsonNode.Type.ARRAY) {
            if (!currentToken.startsWith("[")) {
                throw new IllegalArgumentException("Node '" + name + "' is of type array. Use '" + name + "[#]' or '" + name + "[0,...,n]' instead");
            }
            name = name + currentToken;
            String element = currentToken.substring(1, currentToken.length() - 1);

            if (!element.equals("#") && !element.equals("$")) {
                try {
                    Integer index = Integer.valueOf(element);
                    if (index < 0) {
                        throw new IllegalArgumentException("Element '" + element + "' of node '" + name + "' is lower than zero");
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Element '" + element + "' of node '" + name + "' is not an integer or the wilcard numeric symbol '#'");
                }
            }
            return getSubSchema(name, tokens, schema.getItemSchema());
        } else if (type == JsonNode.Type.OBJECT) {

            boolean hasProp = schema.get("properties") != null;
            boolean hasAddp = schema.get("additionalProperties") != null && schema.get("additionalProperties").getNodeType() != JsonNode.Type.BOOLEAN;

            if (hasProp && hasAddp) {
                throw new UnsupportedOperationException("Objects with properties and additionalProperties at the same time are not supported");
            }

            if (currentToken.startsWith("[")) {
                if (currentToken.equals("[#]")) {
                    throw new IllegalArgumentException("Node '" + name + "' is of type 'object', and [#] notation is only supported for 'array' nodes");
                }
                if (hasAddp) {
                    name = name + currentToken;
                    return getSubSchema(name, tokens, schema.getAdditionalPropertySchema());
                } else {
                    throw new IllegalArgumentException("Node '" + name + "' has not additionalProperties, so [] notation is illegal");
                }
            } else {
                if (name.length() > 0) {
                    name = name + "." + currentToken;
                } else {
                    name = currentToken;
                }
                if (hasProp) {
                    return getSubSchema(name, tokens, schema.getPropertySchema(currentToken));
                } else if (hasAddp) {
                    return getSubSchema(name, tokens, schema.getAdditionalPropertySchema());
                } else {
                    return getSubSchema(name, tokens, null);
                }
            }
        } else {
            throw new IllegalArgumentException("Node is leaf but still are tokens remaining: " + currentToken);
        }
    }

    private static class JsonArrayNode implements JsonNode {

        private final List<JsonNode> children;
        private final String payload;

        public JsonArrayNode(List<JsonNode> children) {
            this.children = children;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < children.size(); i++) {
                JsonNode child = children.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(child);
            }
            sb.append("]");
            this.payload = sb.toString();
        }

        @Override
        public JsonNode.Type getNodeType() {
            return JsonNode.Type.ARRAY;
        }

        @Override
        public Boolean asBoolean() {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public Integer asInteger() {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public Long asLong() {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public Double asDouble() {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public String asString() {
            return toString();
        }

        @Override
        public int getSize() {
            return children.size();
        }

        @Override
        public JsonNode get(int i) {
            return children.get(i);
        }

        @Override
        public Iterator<String> getProperties() {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public JsonNode get(String property) {
            throw new UnsupportedOperationException("Node is of type " + getNodeType());
        }

        @Override
        public String toString() {
            return payload;
        }
    }

    public static void main(String[] args) {
        Expression exp = Expression.compile("$.map[\"akey\"].array[*]");
        System.out.println(exp.getTokens());
    }
}
