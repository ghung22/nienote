package com.lexisnguyen.quicknotie.components.markdown.plugins;

import androidx.annotation.NonNull;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.SimpleBlockNodeVisitor;

public class TableOfContentsPlugin extends AbstractMarkwonPlugin {
    @Override
    public void configure(@NonNull Registry registry) {
        // just to make it explicit
        registry.require(AnchorHeadingPlugin.class);
    }

    @Override
    public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
        builder.on(TableOfContentsBlock.class, new SimpleBlockNodeVisitor());
    }

    @Override
    public void beforeRender(@NonNull Node node) {

        // custom block to hold TOC
        final TableOfContentsBlock block = new TableOfContentsBlock();

        // create TOC title
        {
            final Text text = new Text("Table of contents");
            final Heading heading = new Heading();
            // important one - set TOC heading level
            heading.setLevel(1);
            heading.appendChild(text);
            block.appendChild(heading);
        }

        final HeadingVisitor visitor = new HeadingVisitor(block);
        node.accept(visitor);

        // make it the very first node in rendered markdown (if there are children in this node)
        if (visitor.list.getFirstChild() != null) {
            node.prependChild(block);
        }
    }

    private static class HeadingVisitor extends AbstractVisitor {

        private final ListItem list = new ListItem();
        private final StringBuilder builder = new StringBuilder();
        private boolean isInsideHeading;

        HeadingVisitor(@NonNull Node node) {
            node.appendChild(list);
        }

        @Override
        public void visit(Heading heading) {
            this.isInsideHeading = true;
            try {
                // reset build from previous content
                builder.setLength(0);

                // obtain level (can additionally filter by level, to skip lower ones)
                final int level = heading.getLevel();

                // build heading title
                visitChildren(heading);

                // initial list item
                final Paragraph para = new Paragraph();

                Node parent = para;
                Node node = para;

                for (int i = 1; i < level; i++) {
                    final Paragraph li = new Paragraph();
                    final ListItem item = new ListItem();
                    item.appendChild(li);
                    parent.appendChild(item);
                    parent = li;
                    node = li;
                }

                final String content = builder.toString();
                final Link link = new Link("#" + AnchorHeadingPlugin.createAnchor(content), null);
                final Text text = new Text(content);
                link.appendChild(text);
                node.appendChild(link);
                list.appendChild(para);


            } finally {
                isInsideHeading = false;
            }
        }

        @Override
        public void visit(Text text) {
            // can additionally check if we are building heading (to skip all other texts)
            if (isInsideHeading) {
                builder.append(text.getLiteral());
            }
        }
    }

    private static class TableOfContentsBlock extends CustomBlock {}
}