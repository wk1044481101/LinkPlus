package us.linkpl.linkplus.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import us.linkpl.linkplus.entity.Account;
import us.linkpl.linkplus.entity.AccountSocialmedia;
import us.linkpl.linkplus.entity.Follow;
import us.linkpl.linkplus.entity.SocialMedia;
import us.linkpl.linkplus.entity.response.*;
import us.linkpl.linkplus.mapper.AccountMapper;
import us.linkpl.linkplus.mapper.AccountSocialmediaMapper;
import us.linkpl.linkplus.mapper.FollowMapper;
import us.linkpl.linkplus.mapper.SocialMediaMapper;
import us.linkpl.linkplus.service.impl.AccountSocialmediaServiceImpl;
import us.linkpl.linkplus.service.impl.FollowServiceImpl;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author samsara
 * @since 2021-06-03
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin
public class AccountController {
    @Autowired
    AccountMapper accountMapper;

    @Autowired
    FollowServiceImpl followService;

    @Autowired
    AccountSocialmediaServiceImpl accountSocialmediaService;

    @Autowired
    SocialMediaMapper socialMediaMapper;

    @Autowired
    AccountSocialmediaMapper accountSocialmediaMapper;

    @Autowired
    FollowMapper followMapper;

    @Value("${ACCOUNT}")
    private String ACCOUNT;
    /**
     * 注册
     *
     * @param map
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");
        String nickname = map.get("nickname");
        QueryWrapper<Account> queryWrapper = new QueryWrapper<Account>();
        queryWrapper.eq("username", username).or().eq("nickname", nickname);
        List<Account> accounts = accountMapper.selectList(queryWrapper);
        if (accounts.size() > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String encryption = DigestUtils.md5DigestAsHex(password.getBytes());
        Account account = new Account();
        account.setUsername(username);
        account.setSecretKey(encryption);
        account.setNickname(nickname);
        accountMapper.insert(account);
        return ResponseEntity.ok().build();
    }

    /**
     * 登录
     *
     * @param map     请求信息
     * @param session session
     * @return 状态码
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> map, HttpSession session) {
        String username = map.get("username");
        String password = map.get("password");
        String encryption = DigestUtils.md5DigestAsHex(password.getBytes());
        QueryWrapper<Account> queryWrapper = new QueryWrapper<Account>();
        queryWrapper.eq("username", username);
        List<Account> accounts = accountMapper.selectList(queryWrapper);
        if (accounts.size() == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Account account = accounts.get(0);
        if (username.equals(account.getUsername()) && encryption.equals(account.getSecretKey())) {
            session.setAttribute("accountId", account.getId());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * 注销
     *
     * @param session session
     * @return 状态码
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        Long accountId = (Long) session.getAttribute("accountId");
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        session.removeAttribute("accountId");
        return ResponseEntity.ok().build();
    }

    /**
     * 删除账户
     *
     * @param id 要删除账户的id
     * @return 状态码
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        if (id == null) { //id为空
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Account account = accountMapper.selectById(id);
        if (account == null) { //要删除的账户不存在
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        //session.removeAttribute("accountId");  //退出登录
        accountMapper.deleteById(id);  //删除账户

        QueryWrapper<AccountSocialmedia> queryWrapper = new QueryWrapper<>(); //删除社交媒体中间表中的对应数据
        queryWrapper.eq("accountId", id);
        accountSocialmediaMapper.delete(queryWrapper);

        QueryWrapper<Follow> queryWrapper1 = new QueryWrapper<>(); //删除关注列表和被关注列表
        queryWrapper1.eq("accountId", id).or().eq("followId", id);
        followMapper.delete(queryWrapper1);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/username")
    public ResponseEntity<String> repeatUsername(@RequestParam("username") String username) {
        QueryWrapper<Account> queryWrapper = new QueryWrapper<Account>();
        queryWrapper.eq("username", username);
        List<Account> accounts = accountMapper.selectList(queryWrapper);
        if (accounts.size() == 0) return ResponseEntity.ok().build();
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/nickname")
    public ResponseEntity<String> repeatNickname(@RequestParam("nickname") String nickname) {
        QueryWrapper<Account> queryWrapper = new QueryWrapper<Account>();
        queryWrapper.eq("nickname", nickname);
        List<Account> accounts = accountMapper.selectList(queryWrapper);
        if (accounts.size() == 0) return ResponseEntity.ok().build();
        else return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * 编辑个人信息
     *
     * @param account
     * @return
     */
    @PutMapping("/me")
    public ResponseEntity<String> editAccount(@RequestBody Account account) {
        accountMapper.updateById(account);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询用户信息
     *
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountPage> getAccount(@PathVariable("id") Long id) {
        Account account = accountMapper.selectById(id);

        AccountInfo accountInfo = new AccountInfo();
        BeanUtils.copyProperties(account, accountInfo);

        QueryWrapper<Follow> queryWrapper = new QueryWrapper<Follow>();
        queryWrapper.eq("followId", id);
        int fans = followService.count(queryWrapper);
        queryWrapper = new QueryWrapper<Follow>();
        queryWrapper.eq("accountId", id);
        int follows = followService.count(queryWrapper);
        accountInfo.setFans(fans);
        accountInfo.setFollows(follows);

        List<Media> medias = new ArrayList<>();
        QueryWrapper<AccountSocialmedia> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("accountId", id);
        List<AccountSocialmedia> accountSocialmedia = accountSocialmediaMapper.selectList(queryWrapper1);
        for (AccountSocialmedia a : accountSocialmedia) {
            Media media = new Media();
            int socialMediaId = a.getSocialMediaId();
            SocialMedia socialMedia = socialMediaMapper.selectById(socialMediaId);
            media.setId(a.getId());
            media.setContent(a.getContent());
            media.setMediaName(socialMedia.getName());
            media.setLogoUrl(socialMedia.getLogoUrl());
            medias.add(media);
        }

        AccountPage accountPage = new AccountPage();
        accountPage.setAccountInfo(accountInfo);
        accountPage.setMedias(medias);
        return ResponseEntity.ok(accountPage);
    }

    /**
     * 随机获取用户
     *
     * @param num
     * @return
     */
    @GetMapping("/show")
    public ResponseEntity<List<SimpleAccount>> showAccounts(@RequestParam("num") Integer num) {
        if (num == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<Account> accounts = accountMapper.selectRandomAccount(num);
        List<SimpleAccount> response = new ArrayList<>();
        for (Account account : accounts) {
            SimpleAccount sa = new SimpleAccount();
            sa.setAvatar(account.getAvatar());
            sa.setNickname(account.getNickname());
            sa.setId(account.getId());
            response.add(sa);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 分页展示用户
     *
     * @param params
     * @return
     */
    @GetMapping("/all")
    public ResponseEntity<AccountResponse> showAllAccount(@RequestParam Map<String, Object> params) {
        if (params.get("pageSize") == null || params.get("pageNum") == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int pageSize = Integer.parseInt((String) params.get("pageSize"));
        int pageNum = Integer.parseInt((String) params.get("pageNum"));

        QueryWrapper<Account> queryWrapper = new QueryWrapper<>();
        Page page = new Page<>(pageNum, pageSize);
        IPage<Account> mapIPage = accountMapper.selectPage(page, queryWrapper);
        List<Account> followList = mapIPage.getRecords();

        AccountResponse<Account> accountResponse = new AccountResponse<>();
        accountResponse.setPageNum(pageNum);
        accountResponse.setPageNum(pageSize);
        accountResponse.setTotalPage(mapIPage.getTotal());
        accountResponse.setList(followList);

        return ResponseEntity.ok().body(accountResponse);
    }

    @PostMapping("/avatar")
    public ResponseEntity getAvatar(MultipartFile file,HttpSession session) throws IOException {
        Long accountId = /*5l; */(Long)session.getAttribute("accountId");
       if (Objects.isNull(file) || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();

        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String pic = ACCOUNT + accountId +"_"+suffix;
        String name = accountId +"_"+suffix;

        Path path = Paths.get(pic);
        if (!Files.isWritable(path)) {
            Files.createDirectories(Paths.get(ACCOUNT));
        }
        Files.write(path,bytes);
        Account account = accountMapper.selectById(accountId);
        account.setAvatar("/accounts/avatar/"+name);
        accountMapper.updateById(account);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/background")
    public ResponseEntity getBackground(MultipartFile file,HttpSession session) throws IOException {
        Long accountId = /*5l; */(Long)session.getAttribute("accountId");
        if (Objects.isNull(file) || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();

        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String pic = ACCOUNT + accountId +"_"+suffix;
        String name = accountId +"_"+suffix;

        Path path = Paths.get(pic);
        if (!Files.isWritable(path)) {
            Files.createDirectories(Paths.get(ACCOUNT));
        }
        Files.write(path,bytes);
        Account account = accountMapper.selectById(accountId);
        account.setAvatar("/accounts/background/"+name);
        accountMapper.updateById(account);
        return ResponseEntity.ok().build();
    }
}
