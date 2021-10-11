package com.jyy;

import java.io.PrintStream;
import java.security.Principal;
import java.util.HashMap;

public class insertTest {

    public static void main(String[] args) {
        float[] f = {10, 20, 40, 70,    //0
                30, 10, 70, 15,
               100, 70, 110, 80,        //2
               0, 50, 30, 55,
               13, 21, 54, 78,        //4
               3, 8, 23, 34,//
               200, 29, 202, 50,
               34, 1, 35, 1,            //7
               201, 200, 234, 203,
                56, 69, 58, 70,        //9
			     2, 67, 270, 102,
			     1, 10, 310, 20,		//11
			     23, 12, 345, 120,
			     5, 34, 100, 340,
			     19,100,450,560,	//14
			     12,340,560,450,
			     34,45,190,590,
			     24,47,770,450,	//17
			     91,99,390,980,
			     89,10,99,100,	//19
			     10,29,400,990,
			     110,220,220,330,
			     123,24,234,999	//22
        };
        //HashMap<Integer, Node> hashMap = new HashMap<>();//使用hashmap模拟存储
        //初始化数据
        RTree tree = new RTree();
        tree.Initite(-1, 0, 0, RTree.MaxNODESPACE);

        //开始数据插入
        int j = 0;
        //插入结点
        for (int i = 0; i < f.length; ) {//
            Point p1 = new Point(new float[]{f[i++], f[i++]});
            Point p2 = new Point(new float[]{f[i++], f[i++]});
            Rectangle rectangle = new Rectangle(p1, p2);//生成区域
            //System.out.println("insert " + j + "th " + rectangle + "......");
            tree.insert(rectangle,-2);
            //System.out.println(RTree.hashMap.get(0));
            j++;
        }
        System.out.println("Insert finished.");
        System.out.println("---------------------------------"+RTree.MAXPAGE);
        for(int num =0;num<=RTree.MAXPAGE;num++){
           // System.out.println(RTree.hashMap.get(num));
        }

        //删除结点
        //System.out.println("---------------------------------");
        System.out.println("Begin delete.......");

        j = 0;
        for(int i = 0; i < f.length;) {
            Point p1 = new Point(new float[]{f[i++],f[i++]});
            Point p2 = new Point(new float[]{f[i++],f[i++]});
             Rectangle rectangle = new Rectangle(p1, p2);
            System.out.println("delete " + j + "th " + rectangle + "==========================");
            if(j==2){
                for(int num =0;num<=RTree.MAXPAGE;num++){
                    if(RTree.hashMap.get(num)!=null)
                         System.out.println(RTree.hashMap.get(num));
                }
                System.out.println();
            }
             tree.Delete(rectangle);
            for(int num =0;num<=RTree.MAXPAGE;num++){
                if(RTree.hashMap.get(num)!=null)
                System.out.println(RTree.hashMap.get(num));
            }
            j++;
            System.out.println("==============================================\n");
        }
        //System.out.println(RTree.hashMap.get(0));

    }
}
