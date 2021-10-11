package com.jyy;

import com.sun.media.sound.SoftTuning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class RTree {

    public static final int MaxNODESPACE = 5;//每个节点最大容量
    public static HashMap<Integer,Node> hashMap= new HashMap(); //静态hash用于存储数据
    public static  int MAXPAGE = 0;//为节点生成节点标识

    public void Initite(int parent, int pageNumber, int level,int nodeSpace)
    {
        //hashMap=hashMap;//前者为当前类定义的静态变量，后者为传过来的值
        Node node  = new Node(parent,pageNumber,level,nodeSpace);
        RTree.hashMap.put(0,node);
        //RTree.hashMap.put(RTree.NODESPACE,nodeSpace);//使用key:-3存储节点最大容量
        //RTree.hashMap.put(RTree.MAXPAGE,0);//使用该key标识当前最大的page
    }
    //插入操作
    public void insert(Rectangle rectangle,int page)
    {
        Node leaf;
        int currentPage;//存储叶节点的page标识
        //1.选取根节点
        Node root = (Node)RTree.hashMap.get(0);
        //2.判断当前节点是否为叶子节点
        if(root.isLeaf()) //2.1 如果根节点是叶子节点，则返回
        {
            leaf=root;
            currentPage=0;
        }
        else {
            //2.2如果根节点不是叶子节点，则查找叶子节点，并返回叶子节点-->chooseLeaf()
            leaf = chooseLeaf(root,rectangle);
        }
        //3.在叶子节点中插入值
        if(leaf.getUsedSpace()<RTree.MaxNODESPACE)//3.1如果叶子节点有空间可以放置数据，则直接放入数据
        {
            leaf.datas[leaf.usedSpace]=rectangle;//存入
            leaf.branches[leaf.usedSpace]= page;
            leaf.usedSpace++;
            hashMap.put(leaf.pageNumber,leaf);//更新
            Node parent = leaf.getParent();
            if(parent!=null) //如果当前节点不是根节点，则需要对其父节点进行调整
                parent.adjustTree(leaf,null);//调整
        }
        else{//3.2如果叶子节点没有空间放置数据，则进行分裂
             Node[] nodes = SplitLeaf(leaf,rectangle, page);
             Node l = nodes[0];
             Node ll = nodes[1];

             //如果当前节点是根节点
            if(leaf.isRoot())
            {
                l.parent = 0;
                l.pageNumber = -1;
                ll.parent =0;
                ll.pageNumber = -1;
                l = GeneratorPageNumber(l);//获取pageNumber标识
                ll = GeneratorPageNumber(ll);

                // 根节点已满，需要分裂。创建新的根节点,它的pageNumber=0，level=1
                Node node = new Node(-1,0,1, MaxNODESPACE);
                node.addData(l.getNodeRectangle(),l.pageNumber);
                node.addData(ll.getNodeRectangle(),ll.pageNumber);
                RTree.hashMap.put(0,node);//更新根节点
            }
            else {//非根节点
                l.pageNumber = leaf.pageNumber;
                ll.pageNumber = -1;
                l = GeneratorPageNumber(l);//获取pageNumber标识
                ll = GeneratorPageNumber(ll);
                Node parentNode = leaf.getParent();
                parentNode.adjustTree(l, ll);
            }

        }


    }
    //查找叶子节点
    /**
     * <b>步骤CL1：</b>初始化——记R树的根节点为N。<br>
     * <b>步骤CL2：</b>检查叶节点——如果N是个叶节点，返回N<br>
     * <b>步骤CL3：</b>选择子树——如果N不是叶节点，则从N中所有的条目中选出一个最佳的条目F，
     * 选择的标准是：如果E加入F后，F的外廓矩形FI扩张最小，则F就是最佳的条目。如果有两个
     * 条目在加入E后外廓矩形的扩张程度相等，则在这两者中选择外廓矩形较小的那个。<br>
     * <b>步骤CL4：</b>向下寻找直至达到叶节点——记Fp指向的孩子节点为N，然后返回步骤CL2循环运算， 直至查找到叶节点。
     * <p>
     *
     * @param rectangle
     * @return RTDataNode
     */
    public Node chooseLeaf(Node node,Rectangle rectangle)
    {
        int index = findLeastEnlargement(rectangle,node);
        //得到满足条件的节点
        Node node1 = node.getChild(index);
        if(node.level==1){
            //此节点指向叶子节点
            return node1;//返回满足条件的叶节点
        }
        return chooseLeaf(node1,rectangle);
    }
    /**
     * @param rectangle
     * @return 面积增量最小的结点的索引，如果面积增量相等则选择自身面积更小的
     */
    private int findLeastEnlargement(Rectangle rectangle,Node node) {
        double area = Double.POSITIVE_INFINITY;
        int sel = -1;

        for (int i = 0; i < node.usedSpace; i++) {
            double enlargement = node.datas[i].getUnionRectangle(rectangle)
                    .getArea() - node.datas[i].getArea();
            if (enlargement < area) {
                area = enlargement;
                sel = i;
            } else if (enlargement == area) {
                sel = (node.datas[sel].getArea() <= node.datas[i].getArea()) ? sel : i;
            }
        }

        return sel;
    }

    //获取新的pageNumber标识
    public static  Node GeneratorPageNumber(Node node)
    {
        if (node.pageNumber < 0) {
            node.pageNumber = RTree.MAXPAGE + 1;
            RTree.MAXPAGE++;
        }
        RTree.hashMap.put(node.pageNumber,node);
        return node;

    }

    /**
     *
     * @param leaf 需要分裂的节点
     * @param rectangle 待加入的数据
     * @param page 待加入数据的pageNumber标识
     * @return 分裂后的两个新节点
     */
    public Node[] SplitLeaf(Node leaf , Rectangle rectangle,int page){
        //二次方分类方法
        int[][] group = QuadraticSplit(leaf,rectangle,page);//得到分裂后两个新节点各自数据在leaf中的索引

        Node l = new Node(leaf.parent,-1,0,RTree.MaxNODESPACE);
        Node ll = new Node(leaf.parent,-1,0,RTree.MaxNODESPACE);

        int[] group1 = group[0];
        int[] group2 = group[1];

        for (int i = 0; i < group1.length; i++) {
            l.addData(leaf.datas[group1[i]], leaf.branches[group1[i]]);
        }

        for (int i = 0; i < group2.length; i++) {
            ll.addData(leaf.datas[group2[i]], leaf.branches[group2[i]]);
        }
        return new Node[] { l, ll };
    }
    /**
     * <b>分裂结点的平方算法</b>
     * <p>
     * 1、为两个组选择第一个条目--调用算法pickSeeds()来为两个组选择第一个元素，分别把选中的两个条目分配到两个组当中。<br>
     * 2、检查是否已经分配完毕，如果一个组中的条目太少，为避免下溢，将剩余的所有条目全部分配到这个组中，算法终止<br>
     * 3、调用pickNext来选择下一个进行分配的条目--计算把每个条目加入每个组之后面积的增量，选择两个组面积增量差最大的条目索引,
     * 	    如果面积增量相等则选择面积较小的组，若面积也相等则选择条目数更少的组<br>
     *
     * @param rectangle
     *            导致分裂的溢出Rectangle
     * @param page
     * 			      引起分裂的孩子结点被存储的page，如果分裂发生在叶子结点则为-1,
     * @param page 需要分裂的节点
     * @return 两个组中的条目的索引
     */
    public int[][] QuadraticSplit(Node leaf , Rectangle rectangle,int page)
    {
        if(rectangle == null){
            throw new IllegalArgumentException("Rectangle cannot be null");
        }
        leaf.datas[leaf.usedSpace]=rectangle;//先将rectangle添加进入，计算后在进行删除
        leaf.branches[leaf.usedSpace] = page;
        int total = leaf.usedSpace+1;//一个节点中数据总数

        // 标记访问的条目
        int[] mask = new int[total];
        for (int i = 0; i < total; i++) {
            mask[i] = 1;
        }
        //int c = total / 2 + 1;
        int c =total;
        // 每个组只是有total/2个条 TODO？为什么c等于这个值
        // 每个结点最小条目个数
        int minNodeSize = RTree.MaxNODESPACE/2;
        // 至少有两个
        if (minNodeSize < 2)
            minNodeSize = 2;

        // 记录没有被检查的条目的个数
        int rem = total;

        int[] group1 = new int[c];// 记录分配的条目的索引
        int[] group2 = new int[c];// 记录分配的条目的索引
        // 跟踪被插入每个组的条目的索引
        int i1 = 0, i2 = 0;

        int[] seed = QuadraticPickSeeds(leaf);
        group1[i1++] = seed[0];
        group2[i2++] = seed[1];
        rem -= 2;
        mask[group1[0]] = -1;
        mask[group2[0]] = -1;

        //pickNext算法
        while (rem > 0) {
            // 将剩余的所有条目全部分配到group1组中，算法终止
            if (minNodeSize-i1==rem ) {  //
                /**
                 * 计算提示：
                 * 设第一组已分配个数为g1,第二组已分配个数为g2
                 * m-g1=rem,即g1+rem = m ,则此时第二组的个数应该为g2=M/2+1即m+1个。
                 */
                for (int i = 0; i < total; i++)// 总共rem个
                {
                    if (mask[i] != -1)// 还没有被分配
                    {
                        group1[i1++] = i;
                        mask[i] = -1;
                        rem--;
                    }
                }
                // 将剩余的所有条目全部分配到group2组中，算法终止
            } else if (minNodeSize-i2==rem ) {
                for (int i = 0; i < total; i++)// 总共rem个
                {
                    if (mask[i] != -1)// 还没有被分配
                    {
                        group2[i2++] = i;
                        mask[i] = -1;
                        rem--;
                    }
                }
            } else {
                // 求group1中所有条目的最小外包矩形
                Rectangle mbr1 = (Rectangle) leaf.datas[group1[0]].clone();
                for (int i = 1; i < i1; i++) {
                    mbr1 = mbr1.getUnionRectangle(leaf.datas[group1[i]]);
                }
                // 求group2中所有条目的外包矩形
                Rectangle mbr2 = (Rectangle) leaf.datas[group2[0]].clone();
                for (int i = 1; i < i2; i++) {
                    mbr2 = mbr2.getUnionRectangle(leaf.datas[group2[i]]);
                }

                // 找出下一个进行分配的条目
                double dif = Double.NEGATIVE_INFINITY;
                double areaDiff1=0 ,selAreaDiff1= 0, areaDiff2=0,selAreaDiff2 = 0;
                int sel = -1;//记录使两组面积增量之差最大的索引。

                for (int i = 0; i < total; i++) {
                    if (mask[i] != -1)// 还没有被分配的条目
                    {
                        sel =i;
                        // 计算把每个条目加入每个组之后面积的增量，选择两个组面积增量差最大的条目索引
                        Rectangle a = mbr1.getUnionRectangle(leaf.datas[i]);
                        areaDiff1 = a.getArea() - mbr1.getArea();

                        Rectangle b = mbr2.getUnionRectangle(leaf.datas[i]);
                        areaDiff2 = b.getArea() - mbr2.getArea();

                        if (Math.abs(areaDiff1 - areaDiff2) > dif) {
                            dif = Math.abs(areaDiff1 - areaDiff2);
                            sel = i;
                            selAreaDiff1=areaDiff1;
                            selAreaDiff2=areaDiff2;
                        }

                    }

                }
                if (selAreaDiff1 < selAreaDiff2)// 先比较面积增量
                {
                    group1[i1++] = sel;
                } else if (selAreaDiff1 > selAreaDiff2) {
                    group2[i2++] = sel;
                } else if (mbr1.getArea() < mbr2.getArea())// 再比较自身面积
                {
                    group1[i1++] = sel;
                } else if (mbr1.getArea() > mbr2.getArea()) {
                    group2[i2++] = sel;
                } else if (i1 < i2)// 最后比较条目个数
                {
                    group1[i1++] = sel;
                } else if (i1 > i2) {
                    group2[i2++] = sel;
                } else {
                    group1[i1++] = sel;
                }
                mask[sel] = -1;
                rem--;

            }
        }// end while

        int[][] ret = new int[2][];
        ret[0] = new int[i1];
        ret[1] = new int[i2];

        for (int i = 0; i < i1; i++) {
            ret[0][i] = group1[i];
        }
        for (int i = 0; i < i2; i++) {
            ret[1][i] = group2[i];
        }
        return ret;
    }

    /**
     * 选择两个条目作为组中的新成员。
     *  [计算两个条目的面积对应值]对每一对条目 E1 及 E2 组成一个包括 E1I 及
     * E2I 的矩形 J，计算 d=area(J)-area(E1I)-are(E2I)。
     *  [选择浪费最大的对]选择 d 最大的一对条目返回。
     * @param node 已经加入超过容量的节点
     * @return 返回分配索引
     */
    public int[] QuadraticPickSeeds(Node node)
    {
        double inefficiency = Double.NEGATIVE_INFINITY;
        int i1 = 0, i2 = 0;

        //
        for (int i = 0; i < node.usedSpace; i++) {
            for (int j = i + 1; j <= node.usedSpace; j++)// 注意此处的j值
            {
                Rectangle rectangle = node.datas[i].getUnionRectangle(node.datas[j]);//计算能将两点围住的矩形
                double d = rectangle.getArea() - node.datas[i].getArea()
                        - node.datas[j].getArea();

                if (d > inefficiency) {
                    inefficiency = d;
                    i1 = i;
                    i2 = j;
                }
            }
        }
        return new int[] { i1, i2 };
    }


    /**
     * 算法 Delete
     * 从 R 树中删除索引记录 E。
     *  [找到包含记录的节点]调用 Find Leaf 找到包含 E 的叶节点 L，若果没有找
     * 到记录则停止。
     *  [删除记录]从 L 中删除 E。
     *  [传递变化]调用 Condense Tree，经过 L。
     *  [降低书]如果根节点在经过树的调整之后仅有一个子节点，将这个子节点作
     * 为新的根节点。
     * @param rectangle 要删除的坐标区域
     */
    //节点的删除操作
   public int  Delete(Rectangle rectangle)
    {
        Node root = RTree.hashMap.get(0);

        Node leaf = root.findLeaf(rectangle);

        if (leaf != null) {
            //进行删除操作
            for (int i = 0; i < leaf.usedSpace; i++) {
                if (leaf.datas[i].equals(rectangle)) {
                    int pointer = leaf.branches[i];
                    //deleteData(i);删除节点
                    if (leaf.datas[i + 1] != null) {//如果删除的节位置之后还有数据
                        //将节点中数据和branches中的数据前移
                        System.arraycopy(leaf.datas, i + 1, leaf.datas, i, leaf.usedSpace - i - 1);
                        System.arraycopy(leaf.branches, i + 1, leaf.branches, i, leaf.usedSpace - i - 1);
                        leaf.datas[leaf.usedSpace - 1] = null;//置空
                        leaf.branches[leaf.usedSpace - 1] = -2;
                    } else {//如果要删除的节点位置之后没有数据
                        leaf.datas[i] = null;
                        leaf.branches[i] = -2;
                    }
                    leaf.usedSpace--;
                    //删除完成

                    RTree.GeneratorPageNumber(leaf);// 删除数据后需要重新写入，即内容有变化需重新写入
                    //保存之后进行调整
                    Node parent = leaf.getParent();
                    if(parent!=null) //如果当前节点不是根节点，则需要对其父节点进行调整
                        parent.adjustTree(leaf,null);//调整

                    List<Node> deleteEntriesList = new ArrayList<Node>();
                    condenseTree(deleteEntriesList,leaf);


                    // 重新插入删除结点中剩余的条目
                    for (int j = 0; j < deleteEntriesList.size(); j++) {
                        Node node = deleteEntriesList.get(j);
                        if (node.isLeaf())// 叶子结点，直接把其上的数据重新插入
                        {
                            for (int k = 0; k < node.usedSpace; k++) {
                                insert(node.datas[k], node.branches[k]);
                            }
                        } else {// 非叶子结点，需要先后序遍历出其上的所有结点
                            List<Node> traNodes = traversePostOrder(node);

                            // 把其中的叶子结点中的条目重新插入
                            for (int index = 0; index < traNodes.size(); index++) {
                                Node traNode = traNodes.get(index);
                                if (traNode.isLeaf()) {
                                    for (int t = 0; t < traNode.usedSpace; t++) {
                                        insert(traNode.datas[t],
                                                traNode.branches[t]);
                                    }
                                }
                                if (node != traNode)//
                                    //tree.file.deletePage(traNode.pageNumber);
                                    RTree.hashMap.put(traNode.pageNumber,null);
                                else {
                                    System.out.println("两者相等。。。。");
                                }
                            }// end for
                        }// end else
                        //tree.file.deletePage(node.pageNumber);
                        RTree.hashMap.put(node.pageNumber,null);
                    }// end for

                    return pointer;
                }// end if
            }// end for
            return -1;
        }//产出操作结束
       return -1;
    }

    /**
     * 树的压缩。叶节点L中刚刚删除了一个条目。如果这个节点的条目数太少，则删除该结点，
     * 同时将这些条目重定位到其他节点中。如果有必要，要逐级向上进行这种删除。 调整向上传递的路径上的所有外廓矩形，使其尽可能小，直到根节点。
     * <p>
     * <b>步骤CT1：</b>初始化——记N=L，定义Q为删除节点的集合，初始化的时候将此数组置空。<br>
     * <b>步骤CT2：</b>查找父条目，注意是父条目，不是父节点——如果N是根节点，转到步骤CT6。
     * 如果N不是根节点，记P为N的父节点，并记En为P中代表N的那个条目。<br>
     * <b>步骤CT3：</b>删除下溢结点——如果N中的条目数小于m，意味着节点N下溢，此时应当将En从P中移除，并将N加入Q。<br>
     * <b>步骤CT4：</b>调整外廓矩形——如果N没有被删除，则调整En的外廓矩形EnI，使其尽量变小、恰好包含N中的所有条目。<br>
     * <b>步骤CT5：</b>向上一层——令N=P，返回步骤CT2重新执行。<br>
     * <b>步骤CT6：</b>重新插入孤立条目——对Q中所有节点的所有条目执行重新插入。叶节点中的条目
     * 使用算法Insert重新插入到树的叶节点中；较高层节点中的条目必须插入到树的较高位置上。
     * 这是为了保证这些较高层节点下的子树的叶子节点、与其他叶子节点能够放置在同一层上。<br>
     * --------------------------------------------------------------------
     * <p>
     * 叶节点L中刚刚删除了一个条目，如果这个结点的条目数太少而下溢，则删除该结点，同时将该结点中剩余的条目重定位到其他结点中。
     * 如果有必要，要逐级向上进行这种删除，调整向上传递的路径上的所有外包矩形，使其尽可能小，直到根节点。
     *
     * @param list
     *            存储删除结点中剩余条目
     */
    protected void condenseTree(List<Node> list,Node node) {
        if (node.isRoot()) {
            // 根节点只有一个条目了，即只有左孩子或者右孩子
            if (!node.isLeaf() && node.usedSpace == 1) {
                //如果前节点只剩一个孩子，则删除当前节点，用其子节点变成父节点
                Node n = RTree.hashMap.get(node.branches[0]);
                //deletePage(n.pageNumber);
                RTree.hashMap.put(n.pageNumber,null);
                n.pageNumber = 0;//使子节点成为父节点
                n.parent = -1;//?
                RTree.GeneratorPageNumber(n);//将孩子写入
                if (!n.isLeaf()) {//如果当前节点的子节点不是叶子节点，因为已经改变当前节点的子节点为根节点，所以当前节点的子节点的孩子节点需要更新其父节点
                    for (int i = 0; i < n.usedSpace; i++) {
                        Node m = ((Node)n).getChild(i);
                        m.parent = 0;//?
                        RTree.GeneratorPageNumber(m);//parent属性有变化重新写入
                    }
                }
            }
        } else {
            Node p = node.getParent();
            int e;//得到当前node在父节点中的位置
            //在父节点中找到此结点的条目
            for (e = 0; e < p.usedSpace; e++) {
                if (node.pageNumber == p.branches[e])
                    break;
            }

            int min = RTree.MaxNODESPACE/2;
            if(min<2) min =2;
            if (node.usedSpace < min) {//节点存储的数据个数少于最小个数
                //p.deleteData(e);
                // 从父节点中删除该节点指向
                if (p.datas[e + 1] != null) {//如果删除的节位置之后还有数据
                    //将节点中数据和branches中的数据前移
                    System.arraycopy(p.datas, e + 1, p.datas, e, p.usedSpace - e - 1);
                    System.arraycopy(p.branches, e + 1, p.branches, e, p.usedSpace - e - 1);
                    p.datas[p.usedSpace - 1] = null;//置空
                    p.branches[p.usedSpace - 1] = -2;
                } else {//如果要删除的节点位置之后没有数据
                    p.datas[e] = null;
                    p.branches[e] = -2;
                }
                p.usedSpace--;
                RTree.hashMap.put(p.getPageNumber(),p);
                //删除完成
                list.add(node);// 把数据放入带分配集合中
            } else {//满足最小存储则更新父节点指向该节点的范围
                p.datas[e] = node.getNodeRectangle();
            }
            RTree.GeneratorPageNumber(p);//写回
            //变化之后进行调整
            Node parent = node.getParent();
            if(parent!=null) //如果当前节点不是根节点，则需要对其父节点进行调整
                parent.adjustTree(node,null);//调整
            condenseTree(list,p);//上升检查范围直至根节点
        }
    }
    /**
     * 从给定的结点root开始后序遍历所有的结点
     *
     * @param root
     * @return 所有遍历的结点集合
     */
    public List<Node> traversePostOrder(Node root) {
        if (root == null)
            throw new IllegalArgumentException("Node cannot be null.");

        List<Node> list = new ArrayList<Node>();

        if (!root.isLeaf()) {
            for (int i = 0; i < root.usedSpace; i++) {
                List<Node> a = traversePostOrder(((Node) root)
                        .getChild(i));
                for (int j = 0; j < a.size(); j++) {
                    list.add(a.get(j));
                }
            }
        }

        list.add(root);

        return list;
    }
}
