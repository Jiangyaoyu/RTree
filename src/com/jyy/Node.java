package com.jyy;

import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public class Node {

    public int level; //层次
    public Rectangle[] datas;//存储的数据集合即 最小边界矩形
    public int usedSpace;//已使用的空间
    public int[] branches;//已存储的数据所指向的子节点，如果没有子节点则为-2
    public int pageNumber;//当前节点的page标识，全局唯一
    public int parent;//父节点标识，如果是根节点则标识为-1

    protected Node(int parent, int pageNumber, int level,int nodeSpace) {
        this.parent = parent;
        this.pageNumber = pageNumber;
        this.level = level;
        this.datas = new Rectangle[RTree.MaxNODESPACE+1];
        this.branches = new int[nodeSpace + 1];
        this.usedSpace = 0;
    }
    public Rectangle getNodeRectangle() {
        if (this.usedSpace > 0) {
            Rectangle[] rectangles = new Rectangle[this.usedSpace];
            System.arraycopy(datas, 0, rectangles, 0, usedSpace);
            Rectangle ret = Rectangle.getUnionRectangle(rectangles);
//			if (ret.getHilbertValue() > lhv) {
//				lhv = ret.getHilbertValue();
//			}
            return ret;
        } else {
            return new Rectangle(new Point(new float[] { 0, 0 }),
                    new Point(new float[] { 0, 0 }));
        }
    }
    public void addData(Rectangle rectangle,int pageNumber){
        this.datas[usedSpace]=rectangle;
        this.branches[usedSpace] = pageNumber;
        this.usedSpace++;
    }
    public Node getParent() {
        if (isRoot()) {
            return null;
        } else {
            return RTree.hashMap.get(this.parent);
        }
    }
    public void  adjustTree(Node node1, Node node2) {
        // 先要找到指向原来旧的结点（即未添加Rectangle之前）的条目的索引
        for (int i = 0; i < this.usedSpace; i++) {
            if (this.branches[i] == node1.pageNumber) {
                this.datas[i] = node1.getNodeRectangle();// 更新数据
//				tree.file.writeNode(this);//新加的
                RTree.hashMap.put(this.pageNumber,this);//向总数据集中更新数据
                break;
            }
        }

        if (node2 == null) {
            //tree.file.writeNode(this);
            RTree.GeneratorPageNumber(this);
        }
        /*
         * 如果发生分裂我们必须插入新的结点，否则我们必须继续调整tree直到碰到root结点。
         */
        if (node2 != null) {
            insert(node2);// 插入新的结点

        } else if (!isRoot())// 还没到达根节点
        {
            Node parent = RTree.hashMap.get(this.parent);
            parent.adjustTree(this,null);
        }
    }/**
     * 如果插入结点后导致上溢则需要分裂，<br>
     * 否则不需要分裂，只需更新数据并重新写入file，最后adjustTree()
     *
     * @param node
     * @return 如果结点需要分裂则返回true
     */
    protected boolean insert(Node node) {
        if (usedSpace < RTree.MaxNODESPACE) {
            datas[usedSpace] = node.getNodeRectangle();
            branches[usedSpace] = node.pageNumber;
            usedSpace++;
            node.parent = pageNumber;
            //tree.file.writeNode(node);
            //tree.file.writeNode(this);
            RTree.GeneratorPageNumber(node);
            RTree.GeneratorPageNumber(this);
            /* 先获取其父节点，然后从其父节点开始调整树结构 */
            Node parent = (Node) getParent();
            if (parent != null) {
                parent.adjustTree(this, null);
            }
            return false;
        } else {// 非叶子结点需要分裂
            Node[] a = splitIndex(node);
            Node n = a[0];
            Node nn = a[1];

            if (isRoot()) {
                n.parent = 0;// 其父节点为根节点
                n.pageNumber = -1;
                nn.parent = 0;
                nn.pageNumber = -1;
                /*
                 * 先将分裂后的结点写入file，它会返回一个存储page，然后遍历孩子结点，
                 * 将孩子结点的parent指针指向此结点，然后将孩子结点重新写入file中
                 */
                int p = RTree.GeneratorPageNumber(n).pageNumber;
                for (int i = 0; i < n.usedSpace; i++) {
                    Node ch = n.getChild(i);
                    ch.parent = p;
                    //tree.file.writeNode(ch);
                    RTree.GeneratorPageNumber(ch);
                }
                p = RTree.GeneratorPageNumber(nn).pageNumber;
                for (int i = 0; i < nn.usedSpace; i++) {
                    Node ch = nn.getChild(i);
                    ch.parent = p;
                    //tree.file.writeNode(ch);
                    RTree.GeneratorPageNumber(ch);
                }
                // 新建根节点，层数加1
                Node newRoot = new Node( -1, 0,
                        level + 1,RTree.MaxNODESPACE);
                newRoot.addData(n.getNodeRectangle(), n.pageNumber);
                newRoot.addData(nn.getNodeRectangle(), nn.pageNumber);
                RTree.GeneratorPageNumber(newRoot);

            } else {// not root node, but need split
                n.pageNumber = pageNumber;
                n.parent = parent;
                nn.pageNumber = -1;
                nn.parent = parent;
                RTree.GeneratorPageNumber(n);
                int j = RTree.GeneratorPageNumber(nn).pageNumber;
                for (int i = 0; i < nn.usedSpace; i++) {
                    Node ch = nn.getChild(i);
                    ch.parent = j;
                    RTree.GeneratorPageNumber(ch);
                }
                Node p = (Node) getParent();
                p.adjustTree(n, nn);
            }
        }
        return true;
    }
    /**
     * 非叶子结点的分裂
     *
     * @param node
     * @return
     */
    private Node[] splitIndex(Node node) {
        int[][] group = null;
        group=new RTree().QuadraticSplit(this,node.getNodeRectangle(), node.pageNumber);


        Node index1 = new Node( parent, pageNumber, level,RTree.MaxNODESPACE);
        Node index2 = new Node( parent, -1, level,RTree.MaxNODESPACE);

        int[] group1 = group[0];
        int[] group2 = group[1];

        for (int i = 0; i < group1.length; i++) {
            index1.addData(datas[group1[i]], branches[group1[i]]);
        }
        for (int i = 0; i < group2.length; i++) {
            index2.addData(datas[group2[i]], branches[group2[i]]);
        }

        return new Node[] { index1, index2 };
    }
    public Node getChild(int index) {
        if (index < 0 || index >= usedSpace) {
            throw new IndexOutOfBoundsException("" + index);
        }
        return RTree.hashMap.get(branches[index]);
    }

    //查找叶子节点
    public Node findLeaf(Rectangle rectangle)
    {
        for (int i = 0; i < usedSpace; i++) {
            if (datas[i].enclosure(rectangle)) {
                if(this.level==0){//如果当前节点是叶子节点，直接返回
                    if(datas[i].equals(rectangle))//当查找到叶子层时应该比较时候完全覆盖。
                        return this;

                }else{//如果不是叶子节点则继续
                    Node leaf = getChild(i).findLeaf(rectangle);
                    if(leaf !=null)
                        return leaf;
                }

            }
        }
        return null;
    }



    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Rectangle[] getDatas() {
        return datas;
    }

    public void setDatas(Rectangle[] datas) {
        this.datas = datas;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }

    public int[] getBranches() {
        return branches;
    }

    public void setBranches(int[] branches) {
        this.branches = branches;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }


    public void setParent(int parent) {
        this.parent = parent;
    }

    public Boolean isRoot()
    {
        if(parent==-1)
            return true;
        else return false;
    }
    public Boolean isLeaf()
    {
        if(level==0) return true;
        else return false;
    }
    @Override
    public String toString() {
        String s = "< Page: " + this.pageNumber + ", Level: " + this.level
                + ", UsedSpace: " + this.usedSpace + ", Parent: " + this.parent + " >\n";

        for (int i = 0; i < this.usedSpace; i++) {
            s += "  " + (i + 1) + ") " + this.datas[i].toStr() + " --> " + " page: " + this.branches[i] + "\n";
        }

        return s;
    }
}
