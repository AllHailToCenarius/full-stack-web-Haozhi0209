package edu.infsci2560.services.impl;

import com.blade.ioc.annotation.Inject;
import com.blade.ioc.annotation.Service;
import com.blade.jdbc.ActiveRecord;
import com.blade.jdbc.core.Take;
import com.blade.jdbc.model.Paginator;
import com.blade.kit.BeanKit;
import com.blade.kit.DateKit;
import com.blade.kit.EncrypKit;
import com.blade.kit.StringKit;
import edu.infsci2560.Types;
import edu.infsci2560.exception.TipException;
import edu.infsci2560.models.LoginUser;
import edu.infsci2560.models.User;
import edu.infsci2560.models.UserInfo;
import edu.infsci2560.services.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Inject
    private ActiveRecord activeRecord;

    @Inject
    private ActivecodeService activecodeService;

    @Inject
    private TopicService topicService;

    @Inject
    private UserinfoService userinfoService;


    @Inject
    private CommentService commentService;

    @Override
    public User getUser(Integer uid) {
        if (null == uid) {
            return null;
        }
        return activeRecord.byId(User.class, uid);
    }

    @Override
    public User getUser(Take take) {
        if (null == take) {
            return null;
        }
        return activeRecord.one(take);
    }

    @Override
    public Paginator<User> getPageList(Take take) {
        if (null != take) {
            return activeRecord.page(take);
        }
        return null;
    }

    @Override
    public User signup(String loginName, String passWord, String email) throws Exception {
        if (StringKit.isBlank(loginName) || StringKit.isBlank(passWord) || StringKit.isBlank(email)) {
            return null;
        }

        if (hasUser(loginName)) {
            return null;
        }

        int time = DateKit.getCurrentUnixTime();
        String pwd = EncrypKit.md5(loginName + passWord);
        String avatar = "avatar/default/" + StringKit.getRandomNumber(1, 5) + ".png";

        try {
            User user = new User();
            user.setLogin_name(loginName);
            user.setPass_word(pwd);
            user.setEmail(email);
            user.setAvatar(avatar);
            user.setStatus(0);
            user.setCreate_time(time);
            user.setUpdate_time(time);
            Long uid = activeRecord.insert(user);
            if (null != uid) {
                user.setUid(uid.intValue());

                userinfoService.save(user.getUid());

                String code = activecodeService.save(user, Types.signup.toString());

            }
            return user;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public boolean delete(Integer uid) {
        if (null != uid) {
            activeRecord.delete(User.class, uid);
            return true;
        }
        return false;
    }

    @Override
    public User signin(String loginName, String passWord) {
        if (StringKit.isBlank(loginName) || StringKit.isBlank(passWord)) {
            throw new TipException("Username and password cannot be empty.");
        }

        boolean hasUser = this.hasUser(loginName);
        if (!hasUser) {
            throw new TipException("User not exists.");
        }
        String pwd = EncrypKit.md5(loginName + passWord);
        Take take = new Take(User.class);
        take.eq("pass_word", pwd)
                .in("status", 0, 1)
                .eq("login_name", loginName)
                .or("email", "=", loginName);

        User user = activeRecord.one(take);
        if (null == user) {
            throw new TipException("Username or password error.");
        }

        if (user.getStatus() == 0) {
            throw new TipException("User not activated.");
        }
        return user;
    }

    @Override
    public Map<String, Object> getUserDetail(Integer uid) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (null != uid) {
            User user = activeRecord.byId(User.class, uid);
            UserInfo userInfo = activeRecord.byId(UserInfo.class, uid);
            map = BeanKit.beanToMap(userInfo);
            map.put("username", user.getLogin_name());
            map.put("uid", uid);
            map.put("email", user.getEmail());
            map.put("avatar", user.getAvatar());
            map.put("create_time", user.getCreate_time());
        }
        return map;
    }

    @Override
    public boolean updateStatus(Integer uid, Integer status) {
        if (null != uid && null != status) {
            User temp = new User();
            temp.setUid(uid);
            temp.setStatus(status);
            activeRecord.update(temp);
        }
        return false;
    }

    @Override
    public boolean update(User user) {
        if (null != user && null != user.getUid()) {
            return activeRecord.update(user) > 0;
        }
        return false;
    }

    @Override
    public boolean updatePwd(Integer uid, String newpwd) {
        try {
            if (null == uid || StringKit.isBlank(newpwd)) {
                return false;
            }
            User user = new User();
            user.setUid(uid);
            user.setPass_word(newpwd);
            activeRecord.update(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public LoginUser getLoginUser(User user, Integer uid) {
        if (null == user) {
            user = this.getUser(uid);
        }
        if (null != user) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUid(user.getUid());
            loginUser.setUser_name(user.getLogin_name());
            loginUser.setPass_word(user.getPass_word());
            loginUser.setStatus(user.getStatus());
            loginUser.setRole_id(user.getRole_id());
            loginUser.setAvatar(user.getAvatar());

            Integer comments = commentService.getComments(user.getUid());
            loginUser.setComments(comments);

            Integer topics = topicService.getTopics(user.getUid());
            loginUser.setTopics(topics);

            UserInfo userInfo = userinfoService.getUserinfo(user.getUid());
            if (null != userInfo) {
                loginUser.setJobs(userInfo.getJobs());
                loginUser.setNick_name(userInfo.getNick_name());
            }

            return loginUser;
        }
        return null;
    }

    @Override
    public boolean hasUser(String login_name) {
        if (StringKit.isNotBlank(login_name)) {
            Take take = new Take(User.class);
            take.in("status", 0, 1)
                    .eq("login_name", login_name)
                    .or("email", "=", login_name);

            return activeRecord.count(take) > 0;
        }
        return false;
    }

    @Override
    public User getUserByLoginName(String user_name) {
        if (StringKit.isNotBlank(user_name)) {
            Take take = new Take(User.class);
            take.in("status", 0, 1)
                    .eq("login_name", user_name)
                    .or("email", "=", user_name);

            return activeRecord.one(take);
        }
        return null;
    }

    @Override
    public boolean updateRole(Integer uid, Integer role_id) {
        try {
            if (null == uid || null == role_id || role_id == 1) {
                return false;
            }
            User temp = new User();
            temp.setUid(uid);
            temp.setRole_id(role_id);
            activeRecord.update(temp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
