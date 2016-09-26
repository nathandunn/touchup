
package org.bbop.phylo.io;

import java.io.IOException;

import org.bbop.phylo.model.Tree;

public interface IteratingPhylogenyParser {

    public void reset() throws IOException;

    public Tree next() throws IOException;

    public boolean hasNext();

    public void setSource( final Object o ) throws IOException;
}
