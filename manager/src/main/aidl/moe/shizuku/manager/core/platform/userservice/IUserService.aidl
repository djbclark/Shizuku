package moe.shizuku.manager.core.platform.userservice;

interface IUserService {

    void destroy() = 16777114;
    void exit() = 1;

    List<UserInfo> getUsers() = 2;

    List<ApplicationInfo> getInstalledApplicationsAsUser(int userId) = 3;
}
