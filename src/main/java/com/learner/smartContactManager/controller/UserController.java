package com.learner.smartContactManager.controller;

import com.learner.smartContactManager.dao.ContactRepository;
import com.learner.smartContactManager.dao.UserRepository;
import com.learner.smartContactManager.entities.Contact;
import com.learner.smartContactManager.entities.User;
import com.learner.smartContactManager.helper.Message;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String userName = principal.getName();
        User user = userRepository.getUserByUserName(userName);
        model.addAttribute("user",user);
    }
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
        model.addAttribute("title","User Dashboard");
        return "normal/dashBoard";
    }

    @RequestMapping("/addContact")
    public String openAddContactForm(Model model){
        model.addAttribute("title","Add Contact");
        model.addAttribute("contact",new Contact());

        return "normal/addContact";
    }

    @PostMapping("/processContact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage")MultipartFile file, Principal principal, HttpSession session){
        try{
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);

        if(file.isEmpty()){
            contact.setImage("default.png");
        }else{
            contact.setImage(file.getOriginalFilename());
            File saveFile = new ClassPathResource("static/img").getFile();
            Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
            Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
        }

        contact.setUser(user);
        user.getContacts().add(contact);
        userRepository.save(user);

        session.setAttribute("message",new Message("contact added successfully !!","success"));
        }catch (Exception e){
            session.setAttribute("message",new Message("Something went wrong !!","danger"));
            System.out.println("Error : "+e.getMessage());
            e.printStackTrace();
        }
        return "normal/addContact";
    }

    @RequestMapping("/showContacts/{page}")
    public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal){
        model.addAttribute("title","Show Contacts");
        String userName = principal.getName();
        User user = userRepository.getUserByUserName(userName);

        Pageable pageable = PageRequest.of(page,5);
        Page<Contact> contacts = contactRepository.findContactByUser(user.getId(),pageable);
        model.addAttribute("contacts",contacts);
        model.addAttribute("currentPage",page);
        model.addAttribute("totalPages",contacts.getTotalPages());
        return "normal/showContacts";
    }

    @RequestMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal){
        Optional<Contact> contactOptional = contactRepository.findById(cId);
        Contact contact = contactOptional.get();

        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);

        if(user.getId()==contact.getUser().getId()){
            model.addAttribute("title",contact.getName());
            model.addAttribute("contact",contact);
        }


         return "normal/showContactDetail";
    }

    @RequestMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId") Integer cId,HttpSession session,Model model,Principal principal){
        Contact contact = contactRepository.findById(cId).get();
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        if(user.getId()==contact.getUser().getId()){
            user.getContacts().remove(contact);  //it use equals object internally...
            userRepository.save(user);
            session.setAttribute("message",new Message("User has been deleted successfully!","success"));
        }
        return "redirect:/user/showContacts/0";
    }
    @PostMapping("/updateContact/{cId}")
    public String updateContact(@PathVariable("cId") Integer cId, Model model){
        Contact contact = contactRepository.findById(cId).get();
        model.addAttribute("title","Update Contact");
        model.addAttribute("contact",contact);
        return "normal/updateContact";
    }

    @RequestMapping(value = "/processUpdate",method = RequestMethod.POST)
    public String processUpdate(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,Principal principal,HttpSession session){
        Contact oldContactDetail = contactRepository.findById(contact.getcId()).get();
        try{
             if(!file.isEmpty()){
//                 delete existing image
                 File deleteFile = new ClassPathResource("static/img").getFile();
                 File file1 = new File(deleteFile,oldContactDetail.getImage());
                 file1.delete();



//                 update new image
                 File saveFile = new ClassPathResource("static/img").getFile();
                 Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
                 Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
                 contact.setImage(file.getOriginalFilename());
             }else{
               contact.setImage(oldContactDetail.getImage());
             }
             User user = userRepository.getUserByUserName(principal.getName());
             contact.setUser(user);
             contactRepository.save(contact);
             session.setAttribute("message",new Message("Your contact has been updated !","success"));
         }catch (Exception e){
             System.out.println(e.getMessage());
             e.printStackTrace();
         }

        return "redirect:/user/"+contact.getcId()+"/contact";
    }
    @RequestMapping("/profile")
    public String profile(Model model){
        model.addAttribute("title","Profile");
        return "normal/profile";
    }

    @RequestMapping("/settings")
    public String settings(Model model){
        model.addAttribute("title","Settings");
        return "normal/settings";
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session){
        User user = userRepository.getUserByUserName(principal.getName());
        if (bCryptPasswordEncoder.matches(oldPassword,user.getPassword())){
            user.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userRepository.save(user);
            session.setAttribute("message",new Message("Your password has been updated !","success"));
        }else{
            session.setAttribute("message",new Message("Wrong old Password","danger"));
            return "redirect:/user/settings";
        }

        return "redirect:/user/index";
    }
}
