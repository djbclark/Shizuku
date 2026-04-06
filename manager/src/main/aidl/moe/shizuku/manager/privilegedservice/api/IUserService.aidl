package moe.shizuku.manager.privilegedservice.api;

interface IUserService {

    void destroy() = 16777114;
    void exit() = 1;

    List<UserInfo> getUsers() = 2;

    List<PackageInfo> getInstalledPackagesAsUser(int userId) = 3;
}
