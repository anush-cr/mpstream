/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mpstream.datastructures;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 *
 * @author anush_cr
 */
public class transaction {
    public final ObjectInputStream in;
    public final ObjectOutputStream out;
    public final PriorityQueue cmdQueue = new PriorityQueue();
    public boolean err;
    public final CopyOnWriteArrayList devices;
    public transaction(ObjectInputStream i, ObjectOutputStream o) {
        this.in = i;
        this.out = o;
        this.err = false;
        this.devices = new CopyOnWriteArrayList();
    }
}
