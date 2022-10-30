/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.yarp;

import org.truffleruby.core.array.ArrayUtils;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.SourceIndexLength;
import org.truffleruby.language.arguments.EmptyArgumentsDescriptor;
import org.truffleruby.language.dispatch.RubyCallNode;
import org.truffleruby.language.dispatch.RubyCallNodeParameters;
import org.truffleruby.language.literal.IntegerFixnumLiteralNode;
import org.truffleruby.parser.Translator;
import org.yarp.AbstractNodeVisitor;
import org.yarp.Nodes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class YARPTranslator extends AbstractNodeVisitor<RubyNode> {

    final byte[] source;

    public YARPTranslator(byte[] source) {
        this.source = source;
    }

    @Override
    public RubyNode visitProgram(Nodes.Program node) {
        return node.statements.accept(this);
    }

    @Override
    public RubyNode visitStatements(Nodes.Statements node) {
        var location = new SourceIndexLength(node.startOffset, node.endOffset - node.startOffset);

        var body = node.body;
        var translated = new RubyNode[body.length];
        for (int i = 0; i < body.length; i++) {
            translated[i] = body[i].accept(this);
        }
        return Translator.sequence(location, Arrays.asList(translated));
    }

    @Override
    public RubyNode visitCallNode(Nodes.CallNode node) {
        var methodName = new String(node.name, StandardCharsets.UTF_8);
        var receiver = node.receiver.accept(this);
        var argumentsNode = (Nodes.ArgumentsNode) node.arguments;
        var arguments = argumentsNode.arguments;
        var translatedArguments = new RubyNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            translatedArguments[i] = arguments[i].accept(this);
        }

        return new RubyCallNode(new RubyCallNodeParameters(receiver, methodName, null,
                EmptyArgumentsDescriptor.INSTANCE, translatedArguments, false, false));
    }

    @Override
    public RubyNode visitIntegerLiteral(Nodes.IntegerLiteral node) {
        var token = node.value;
        byte[] bytes = ArrayUtils.extractRange(source, token.startOffset, token.endOffset);
        int value = Integer.parseInt(new String(bytes, StandardCharsets.US_ASCII));
        return new IntegerFixnumLiteralNode(value);
    }

    @Override
    protected RubyNode defaultVisit(Nodes.Node node) {
        throw new Error("Unknown node: " + node);
    }
}
