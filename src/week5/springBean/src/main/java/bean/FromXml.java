package week5.springBean.src.main.java.bean;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class FromXml {

    public static void main(String[] args) {

        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
//        Student student123 = context.getBean(Student.class);

        io.kimmking.spring01.Student student123 = (week5.springBean.src.main.java.bean.pojo.Student) context.getBean("student123");
        System.out.println(student123.toString());

        student123.print();

        io.kimmking.spring01.Student student100 = (week5.springBean.src.main.java.bean.pojo.Student) context.getBean("student100");
        System.out.println(student100.toString());

        student100.print();
    }
}
