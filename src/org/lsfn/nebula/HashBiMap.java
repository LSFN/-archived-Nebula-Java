package org.lsfn.nebula;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashBiMap<A, B> implements BiMap<A, B> {

    private Map<A,B> mapAB;
    private Map<B,A> mapBA;
    
    public HashBiMap() {
        this.mapAB = new HashMap<A, B>();
        this.mapBA = new HashMap<B, A>();
    }
    
    @Override
    public void addPair(A a, B b) {
        this.mapAB.put(a, b);
        this.mapBA.put(b, a);
    }

    @Override
    public B getBForA(A a) {
        return this.mapAB.get(a);
    }

    @Override
    public A getAForB(B b) {
        return this.mapBA.get(b);
    }

    @Override
    public B removeA(A a) {
        B b = this.mapAB.remove(a);
        this.mapBA.remove(b);
        return b;
    }

    @Override
    public A removeB(B b) {
        A a = this.mapBA.remove(b);
        this.mapAB.remove(a);
        return a;
    }

    @Override
    public Set<A> getAs() {
        return this.mapAB.keySet();
    }

    @Override
    public Set<B> getBs() {
        return this.mapBA.keySet();
    }

}
