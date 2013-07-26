package org.lsfn.nebula;

import java.util.Set;

public interface BiMap<A, B> {

    public void addPair(A a, B b);
    public B getBForA(A a);
    public A getAForB(B b);
    public B removeA(A a);
    public A removeB(B b);
    public Set<A> getAs();
    public Set<B> getBs();
}
