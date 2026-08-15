package com.ymopuri.qsactions.shizuku;

interface IShellService {

    // Reserved by the Shizuku server for tearing the user service down.
    void destroy() = 16777114;

    String exec(String command) = 1;
}
