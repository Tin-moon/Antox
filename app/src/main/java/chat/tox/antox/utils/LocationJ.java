package chat.tox.antox.utils;

import scala.Product;
import scala.Serializable;
import scala.collection.Iterator;
import scala.runtime.BoxesRunTime;
import scala.runtime.ScalaRunTime;

/**
 * Created by Nechypurenko on 13.02.2018.
 */

public class LocationJ implements Product, Serializable {

    public static LocationJ Origin() {
        return new LocationJ(0,0);
    }

    private final int x;
    private final int y;

    public LocationJ(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    @Override
    public boolean canEqual(Object that) {
        return that instanceof LocationJ;
    }

    @Override
    public Integer productElement(int n) {
        int value;
        switch (n) {
            case 0:
                value = this.x();
                break;
            case 1:
                value = this.y();
                break;
            default:
                throw new IndexOutOfBoundsException(BoxesRunTime.boxToInteger(n).toString());
        }

        return value;
    }

    @Override
    public int productArity() {
        return 2;
    }

    @Override
    public String productPrefix() {
        return "LocationJ";
    }

    @Override
    public Iterator<Object> productIterator() {
        return ScalaRunTime.typedProductIterator(this);
    }
}
