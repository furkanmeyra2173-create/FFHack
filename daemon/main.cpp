// language: C++17 | Android ARM64 rooted | Free Fire (com.dts.freefireth)
// build: aarch64-linux-android35-clang++ -O2 -std=c++17 -static-libstdc++ -pie -fPIE -o ffd main.cpp

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <cstdlib>
#include <vector>
#include <string>
#include <thread>
#include <chrono>
#include <random>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/uio.h>

#define CFG_PATH "/data/local/tmp/.hcfg"
#define ESP_PATH "/data/local/tmp/.hesp"
#define SCR_PATH "/data/local/tmp/.hscr"

// ════════════════════════════════════════════
//  FEATURE FLAGS
// ════════════════════════════════════════════
enum Feat : int {
    F_NORECOIL=0, F_SILENTAIM=1, F_AIMBOT=2, F_RAPIDFIRE=3,
    F_NOSWAY=4, F_BULLETSPD=5, F_NOSPREAD=6, F_INFINITEAMMO=7, F_NOZOOM=8,
    F_FASTRELOAD=9, F_MAXHP=10, F_MAXARMOR=11, F_AUTOHEAL=12,
    F_NOKNOCKBACK=13, F_NOFALLDAM=14,
    F_SPEED1=15, F_SPEED2=16, F_SPEED3=17, F_FLYHACK=18,
    F_PARASPEED=19, F_ANTIAIM=20,
    F_VEHSPEED=21, F_VEHICLESP=22,
    F_GHOSTWALL=23, F_ESP=24, F_ITEMESP=25, F_LOOTFILTER=26,
    F_CAMLOCK=27, F_WIDEFOV=28, F_TEAMHP=29, F_DROPZONE=30, F_SAFEZONE=31,
    F_CROSSHAIR=32, F_RADAR=33, F_NIGHTMODE=34, F_NOGRASS=35, F_BRIGHTMODE=36,
    F_COUNT=37
};

static uint8_t g_feat[F_COUNT]{};

void LoadConfig() {
    FILE* f = fopen(CFG_PATH, "rb");
    if (!f) return;
    fread(g_feat, 1, F_COUNT, f);
    fclose(f);
}

// ════════════════════════════════════════════
//  SYSCALLS (ARM64 direct)
// ════════════════════════════════════════════
static inline long sc3(long nr, long a, long b, long c) {
    register long x8 asm("x8") = nr;
    register long x0 asm("x0") = a;
    register long x1 asm("x1") = b;
    register long x2 asm("x2") = c;
    asm volatile("svc #0" : "+r"(x0) : "r"(x8),"r"(x1),"r"(x2) : "memory");
    return x0;
}
static inline long sc6(long nr, long a, long b, long c, long d, long e, long f_) {
    register long x8 asm("x8") = nr;
    register long x0 asm("x0") = a;
    register long x1 asm("x1") = b;
    register long x2 asm("x2") = c;
    register long x3 asm("x3") = d;
    register long x4 asm("x4") = e;
    register long x5 asm("x5") = f_;
    asm volatile("svc #0" : "+r"(x0)
        : "r"(x8),"r"(x1),"r"(x2),"r"(x3),"r"(x4),"r"(x5) : "memory");
    return x0;
}

#undef SYS_openat
#define SYS_openat   56
#undef SYS_pread64
#define SYS_pread64  67
#undef SYS_pwrite64
#define SYS_pwrite64 68
#define SYS_pvm_r    270

static int   g_mfd = -1;
static pid_t g_pid = -1;

template<typename T>
T ReadMem(uintptr_t a) {
    T v{};
    sc6(SYS_pread64, g_mfd, (long)&v, sizeof(T), (long)a, 0, 0);
    return v;
}

template<typename T>
void WriteMem(uintptr_t a, T v) {
    sc6(SYS_pwrite64, g_mfd, (long)&v, sizeof(T), (long)a, 0, 0);
}

uintptr_t Ptr(uintptr_t a) {
    uintptr_t v = ReadMem<uintptr_t>(a);
    return (v > 0x10000 && v < 0x7FFFFFFFFFFF) ? v : 0;
}

// ════════════════════════════════════════════
//  PROCESS UTILS
// ════════════════════════════════════════════
pid_t FindPid(const char* pkg) {
    DIR* d = opendir("/proc");
    if (!d) return -1;
    struct dirent* e;
    while ((e = readdir(d))) {
        pid_t p = atoi(e->d_name);
        if (!p) continue;
        char path[64], buf[256]{};
        snprintf(path, 64, "/proc/%d/cmdline", p);
        FILE* f = fopen(path, "r");
        if (!f) continue;
        fgets(buf, 256, f);
        fclose(f);
        if (strstr(buf, pkg)) {
            closedir(d);
            return p;
        }
    }
    closedir(d);
    return -1;
}

uintptr_t FindBase(pid_t pid, const char* mod) {
    char path[64];
    snprintf(path, 64, "/proc/%d/maps", pid);
    FILE* f = fopen(path, "r");
    if (!f) return 0;
    char line[512];
    while (fgets(line, 512, f)) {
        if (strstr(line, mod) && strstr(line, "r-xp")) {
            uintptr_t b = 0;
            sscanf(line, "%lx-", &b);
            fclose(f);
            return b;
        }
    }
    fclose(f);
    return 0;
}

bool OpenMem(pid_t pid) {
    char p[64];
    snprintf(p, 64, "/proc/%d/mem", pid);
    g_mfd = (int)sc3(SYS_openat, (long)AT_FDCWD, (long)p, O_RDWR);
    return g_mfd >= 0;
}

// ════════════════════════════════════════════
//  MATH
// ════════════════════════════════════════════
struct Vec3 {
    float x, y, z;
    Vec3 operator-(Vec3 o) const { return {x-o.x, y-o.y, z-o.z}; }
    Vec3 operator+(Vec3 o) const { return {x+o.x, y+o.y, z+o.z}; }
    float len() const { return sqrtf(x*x + y*y + z*z); }
    Vec3 norm() const {
        float l = len();
        return l > 0 ? Vec3{x/l, y/l, z/l} : Vec3{};
    }
};
struct Vec2 { float x, y; };
struct Mat4 { float m[4][4]; };

bool W2S(const Mat4& vp, Vec3 w, Vec2& s, float sw, float sh) {
    float x  = vp.m[0][0]*w.x + vp.m[1][0]*w.y + vp.m[2][0]*w.z + vp.m[3][0];
    float y  = vp.m[0][1]*w.x + vp.m[1][1]*w.y + vp.m[2][1]*w.z + vp.m[3][1];
    float ww = vp.m[0][3]*w.x + vp.m[1][3]*w.y + vp.m[2][3]*w.z + vp.m[3][3];
    if (ww < 0.01f) return false;
    s.x = (x / ww + 1.f) * sw * 0.5f;
    s.y = (1.f - y / ww) * sh * 0.5f;
    return true;
}

// ════════════════════════════════════════════
//  OFFSETS
// ════════════════════════════════════════════
namespace Off {
    constexpr uintptr_t GameFacade     = 0xA72E4E0;
    constexpr uintptr_t Facade_P2      = 0xB8;
    constexpr uintptr_t Match          = 0x90;
    constexpr uintptr_t Match_Dict     = 0xC0;
    constexpr uintptr_t Match_Local    = 0xD8;
    constexpr uintptr_t Match_SafeZone = 0x1C0;
    constexpr uintptr_t SZ_Timer       = 0x48;
    constexpr uintptr_t SZ_CenterX     = 0x30;
    constexpr uintptr_t SZ_CenterZ     = 0x38;
    constexpr uintptr_t SZ_Radius      = 0x40;
    constexpr uintptr_t Dict_Ents      = 0x18;
    constexpr uintptr_t Dict_Cnt       = 0x20;
    constexpr uintptr_t Dict_Str       = 0x18;
    constexpr uintptr_t Dict_Val       = 0x10;
    constexpr uintptr_t ItemManager    = 0xE8;
    constexpr uintptr_t ItemList       = 0x18;
    constexpr uintptr_t ItemCount      = 0x20;
    constexpr uintptr_t ItemPos        = 0x90;
    constexpr uintptr_t ItemType       = 0x48;
    constexpr uintptr_t VehicleManager = 0xF0;
    constexpr uintptr_t VehicleList    = 0x18;
    constexpr uintptr_t VehicleCount   = 0x20;
    constexpr uintptr_t VehiclePos     = 0x90;
    constexpr uintptr_t VehicleHP      = 0x74;
    constexpr uintptr_t VehicleSpeed   = 0x2DC;
    constexpr uintptr_t TeamManager    = 0xD0;
    constexpr uintptr_t TeamList       = 0x18;
    constexpr uintptr_t TeamCount      = 0x20;
    constexpr uintptr_t P_Dead         = 0x7C;
    constexpr uintptr_t P_Bot          = 0x4A0;
    constexpr uintptr_t P_HP1          = 0x70;
    constexpr uintptr_t P_HP2          = 0x10;
    constexpr uintptr_t P_HP3          = 0x20;
    constexpr uintptr_t P_HPVal        = 0x18;
    constexpr uintptr_t P_Armor        = 0x74;
    constexpr uintptr_t P_FallDmg      = 0x2E8;
    constexpr uintptr_t P_Knockback    = 0x2EC;
    constexpr uintptr_t P_Firing       = 0x848;
    constexpr uintptr_t P_AimInfo      = 0xE50;
    constexpr uintptr_t Aim_Dir        = 0x40;
    constexpr uintptr_t Aim_Pos        = 0x4C;
    constexpr uintptr_t P_Rotation     = 0x614;
    constexpr uintptr_t P_MoveSpeed    = 0x2DC;
    constexpr uintptr_t P_GravityY     = 0x2E0;
    constexpr uintptr_t P_ParaSpeed    = 0x3A0;
    constexpr uintptr_t P_ZoomPenalty  = 0x3B0;
    constexpr uintptr_t P_Name         = 0x490;
    constexpr uintptr_t P_Weapon       = 0x600;
    constexpr uintptr_t W_Data         = 0x80;
    constexpr uintptr_t W_Recoil       = 0x18;
    constexpr uintptr_t W_FastRld      = 0xD9;
    constexpr uintptr_t W_FireRate     = 0x1C;
    constexpr uintptr_t W_Sway         = 0x90;
    constexpr uintptr_t W_SwayAmt      = 0x24;
    constexpr uintptr_t W_BulletSpd    = 0x30;
    constexpr uintptr_t W_Spread       = 0x20;
    constexpr uintptr_t W_Ammo         = 0xA0;
    constexpr uintptr_t W_MaxAmmo      = 0xA4;
    constexpr uintptr_t B_Head         = 0x6A0;
    constexpr uintptr_t B_Breast       = 0x6B0;
    constexpr uintptr_t B_Hip          = 0x6A8;
    constexpr uintptr_t B_LSho         = 0x708;
    constexpr uintptr_t B_RSho         = 0x710;
    constexpr uintptr_t B_LHand        = 0x720;
    constexpr uintptr_t B_RHand        = 0x718;
    constexpr uintptr_t B_LAnk         = 0x6D8;
    constexpr uintptr_t B_RAnk         = 0x6E0;
    constexpr uintptr_t TF_Int         = 0x10;
    constexpr uintptr_t TF_Idx         = 0x40;
    constexpr uintptr_t TF_Mat         = 0x18;
    constexpr uintptr_t TF_IdxL        = 0x20;
    constexpr uintptr_t P_Camera       = 0x690;
    constexpr uintptr_t Cam_Follow     = 0x28;
    constexpr uintptr_t Cam_Int        = 0x10;
    constexpr uintptr_t Cam_Matrix     = 0x100;
    constexpr uintptr_t Cam_FOV        = 0x78;
    constexpr uintptr_t P_Avatar       = 0x770;
    constexpr uintptr_t Av_Uma         = 0x138;
    constexpr uintptr_t Uma_Vis        = 0x101;
    constexpr uintptr_t GrassManager   = 0x1A0;
    constexpr uintptr_t Grass_Enable   = 0x18;
    constexpr uintptr_t AmbientLight   = 0x1B0;
    constexpr uintptr_t AmbientR       = 0x10;
    constexpr uintptr_t AmbientG       = 0x14;
    constexpr uintptr_t AmbientB       = 0x18;
}

// ════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════
Vec3 BonePos(uintptr_t tf) {
    if (!tf) return {};
    uintptr_t in = Ptr(tf + Off::TF_Int);
    if (!in) return {};
    uintptr_t ia = Ptr(in + Off::TF_Idx);
    if (!ia) return {};
    uintptr_t ml = Ptr(ia + Off::TF_Mat);
    uintptr_t il = Ptr(ia + Off::TF_IdxL);
    if (!ml || !il) return {};
    uint32_t idx = ReadMem<uint32_t>(il + 0x4);
    uintptr_t e  = ml + (uintptr_t)idx * 0x30;
    return {
        ReadMem<float>(e + 0x20),
        ReadMem<float>(e + 0x24),
        ReadMem<float>(e + 0x28)
    };
}

std::string ReadName(uintptr_t ptr) {
    if (!ptr) return "";
    int32_t len = ReadMem<int32_t>(ptr + 0x10);
    if (len <= 0 || len > 32) return "";
    std::string out;
    for (int i = 0; i < len; i++) {
        char16_t c = ReadMem<char16_t>(ptr + 0x14 + (uintptr_t)i * 2);
        out += (c < 128 ? (char)c : '?');
    }
    return out;
}

// ════════════════════════════════════════════
//  ESP STRUCTS
// ════════════════════════════════════════════
struct BP { float sx, sy; };

struct EspEntry {
    BP head, breast, hip;
    BP l_sho, r_sho;
    BP l_hand, r_hand;
    BP l_ank, r_ank;
    float box_top, box_bot, box_cx, box_w;
    float hp, armor, dist;
    char  name[24];
    uint8_t is_bot;
    uint8_t pad[3];
};

struct ItemEntry {
    float   sx, sy, dist;
    uint8_t type;
    char    name[20];
    uint8_t pad[3];
};

struct VehEntry {
    float   sx, sy, dist, hp;
    uint8_t pad[4];
};

struct TeamEntry {
    float hp, armor;
    char  name[20];
};

struct SafeZoneInfo {
    float cx, cz, radius, timer;
};

// ════════════════════════════════════════════
//  JITTER
// ════════════════════════════════════════════
static std::mt19937 rng(42);

void JSlp(int b, int v) {
    std::uniform_int_distribution<int> d(0, v);
    std::this_thread::sleep_for(std::chrono::milliseconds(b + d(rng)));
}

// ════════════════════════════════════════════
//  FEATURES — her biri kendi kapsamında
// ════════════════════════════════════════════
void NoRecoil(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (wd) WriteMem<float>(wd + Off::W_Recoil, 0.f);
}

void RapidFire(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (wd) WriteMem<float>(wd + Off::W_FireRate, 0.01f);
}

void NoSway(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (!wd) return;
    uintptr_t sw = Ptr(wd + Off::W_Sway);
    if (sw) WriteMem<float>(sw + Off::W_SwayAmt, 0.f);
}

void BulletSpeed(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (wd) WriteMem<float>(wd + Off::W_BulletSpd, 9999.f);
}

void NoSpread(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (wd) WriteMem<float>(wd + Off::W_Spread, 0.f);
}

void InfiniteAmmo(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (!wd) return;
    int32_t mx = ReadMem<int32_t>(wd + Off::W_MaxAmmo);
    if (mx > 0) WriteMem<int32_t>(wd + Off::W_Ammo, mx);
}

void NoZoom(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_ZoomPenalty, 1.f);
}

void FastReload(uintptr_t lo) {
    uintptr_t wp = Ptr(lo + Off::P_Weapon);
    uintptr_t wd = Ptr(wp + Off::W_Data);
    if (wd) WriteMem<uint8_t>(wd + Off::W_FastRld, 1);
}

void MaxHP(uintptr_t lo) {
    uintptr_t h3 = Ptr(Ptr(Ptr(lo + Off::P_HP1) + Off::P_HP2) + Off::P_HP3);
    if (h3) WriteMem<float>(h3 + Off::P_HPVal, 200.f);
}

void MaxArmor(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_Armor, 200.f);
}

void AutoHeal(uintptr_t lo) {
    uintptr_t h3 = Ptr(Ptr(Ptr(lo + Off::P_HP1) + Off::P_HP2) + Off::P_HP3);
    if (!h3) return;
    if (ReadMem<float>(h3 + Off::P_HPVal) < 150.f)
        WriteMem<float>(h3 + Off::P_HPVal, 200.f);
}

void NoKnockback(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_Knockback, 0.f);
}

void NoFallDmg(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_FallDmg, 0.f);
}

void SpeedHack(uintptr_t lo, float mul) {
    float cur = ReadMem<float>(lo + Off::P_MoveSpeed);
    if (cur > 0.f && cur < 30.f)
        WriteMem<float>(lo + Off::P_MoveSpeed, cur * mul);
}

void FlyHack(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_GravityY, 0.f);
}

void ParaSpeed(uintptr_t lo) {
    WriteMem<float>(lo + Off::P_ParaSpeed, 999.f);
}

void AntiAIM(uintptr_t lo) {
    std::uniform_real_distribution<float> d(-180.f, 180.f);
    WriteMem<float>(lo + Off::P_Rotation + 4, d(rng));
}

void GhostWall(uintptr_t ep) {
    uintptr_t av = Ptr(ep + Off::P_Avatar);
    uintptr_t um = Ptr(av + Off::Av_Uma);
    if (um) WriteMem<uint8_t>(um + Off::Uma_Vis, 1);
}

void WideFOV(uintptr_t lo) {
    uintptr_t cam = Ptr(lo + Off::P_Camera);
    uintptr_t cfo = Ptr(cam + Off::Cam_Follow);
    if (cfo) WriteMem<float>(cfo + Off::Cam_FOV, 100.f);
}

void NoGrass(uintptr_t facade) {
    uintptr_t gm = Ptr(facade + Off::GrassManager);
    if (gm) WriteMem<uint8_t>(gm + Off::Grass_Enable, 0);
}

void BrightMode(uintptr_t facade) {
    uintptr_t al = Ptr(facade + Off::AmbientLight);
    if (!al) return;
    WriteMem<float>(al + Off::AmbientR, 2.f);
    WriteMem<float>(al + Off::AmbientG, 2.f);
    WriteMem<float>(al + Off::AmbientB, 2.f);
}

void NightMode(uintptr_t facade) {
    uintptr_t al = Ptr(facade + Off::AmbientLight);
    if (!al) return;
    WriteMem<float>(al + Off::AmbientR, 0.05f);
    WriteMem<float>(al + Off::AmbientG, 0.08f);
    WriteMem<float>(al + Off::AmbientB, 0.15f);
}

void CamLock(uintptr_t lo, Vec3 lh, Vec3 th) {
    Vec3 d     = th - lh;
    float hz   = sqrtf(d.x*d.x + d.z*d.z);
    float yaw  =  atan2f(d.x, d.z) * (180.f / 3.14159265f);
    float pitch = -atan2f(d.y, hz)  * (180.f / 3.14159265f);
    pitch = fmaxf(-60.f, fminf(60.f, pitch));
    WriteMem<float>(lo + Off::P_Rotation,     pitch);
    WriteMem<float>(lo + Off::P_Rotation + 4, yaw);
}

void Aimbot(uintptr_t lo, Vec3 lh, Vec3 th) {
    CamLock(lo, lh, th);
    WriteMem<uint32_t>(lo + Off::P_Firing, 1);
}

// ════════════════════════════════════════════
//  MAIN
// ════════════════════════════════════════════
int main(int argc, char** argv) {

    const char* FF_PKG = "com.dts.freefireth";

    // Process name spoof
    const char* fake = "kworker/u8:1";
    if (argc > 0) {
        memset(argv[0], 0, strlen(argv[0]));
        strncpy(argv[0], fake, strlen(argv[0]));
    }
    prctl(PR_SET_NAME, fake, 0, 0, 0);

    // Ekran çözünürlüğü — APK yazar, biz okuruz
    float SW = 1080.f, SH = 2400.f;
    {
        FILE* sf = fopen(SCR_PATH, "rb");
        if (sf) {
            fread(&SW, 4, 1, sf);
            fread(&SH, 4, 1, sf);
            fclose(sf);
        }
    }

    // FF bekleme
    while (g_pid <= 0) {
        g_pid = FindPid(FF_PKG);
        std::this_thread::sleep_for(std::chrono::seconds(2));
    }

    // /proc/pid/mem aç
    while (!OpenMem(g_pid))
        std::this_thread::sleep_for(std::chrono::seconds(1));

    // libil2cpp.so base
    uintptr_t base = 0;
    while (!base) {
        base = FindBase(g_pid, "libil2cpp.so");
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }

    const uintptr_t fa = base + Off::GameFacade;

    // ── ANA DÖNGÜ ──
    while (true) {

        LoadConfig();

        uintptr_t facade = Ptr(fa);
        if (!facade) { JSlp(300, 100); continue; }

        uintptr_t bg    = Ptr(facade + Off::Facade_P2);
        uintptr_t match = Ptr(bg     + Off::Match);
        uintptr_t local = Ptr(match  + Off::Match_Local);
        if (!local || ReadMem<uint32_t>(local + Off::P_Dead)) {
            JSlp(150, 50);
            continue;
        }

        // View matrix
        Mat4 vp{};
        uintptr_t cam = Ptr(local + Off::P_Camera);
        uintptr_t cfo = Ptr(cam   + Off::Cam_Follow);
        uintptr_t cip = Ptr(cfo   + Off::Cam_Int);
        if (cip) {
            struct iovec lv = {&vp, sizeof(vp)};
            struct iovec rv = {(void*)(cip + Off::Cam_Matrix), sizeof(vp)};
            sc6(SYS_pvm_r, g_pid, (long)&lv, 1, (long)&rv, 1, 0);
        }

        // Local bilgiler
        uintptr_t lh3        = Ptr(Ptr(Ptr(local + Off::P_HP1) + Off::P_HP2) + Off::P_HP3);
        float     local_hp   = lh3 ? ReadMem<float>(lh3 + Off::P_HPVal) : 0.f;
        float     local_armor= ReadMem<float>(local + Off::P_Armor);
        Vec3      local_head = BonePos(Ptr(local + Off::B_Head));

        // Safe zone
        SafeZoneInfo szinfo{};
        uintptr_t sz = Ptr(match + Off::Match_SafeZone);
        if (sz) {
            szinfo.cx     = ReadMem<float>(sz + Off::SZ_CenterX);
            szinfo.cz     = ReadMem<float>(sz + Off::SZ_CenterZ);
            szinfo.radius = ReadMem<float>(sz + Off::SZ_Radius);
            szinfo.timer  = ReadMem<float>(sz + Off::SZ_Timer);
        }

        // Team HP
        std::vector<TeamEntry> team_list;
        if (g_feat[F_TEAMHP]) {
            uintptr_t tm = Ptr(match + Off::TeamManager);
            uintptr_t tl = tm ? Ptr(tm + Off::TeamList) : 0;
            int32_t   tc = tm ? ReadMem<int32_t>(tm + Off::TeamCount) : 0;
            for (int i = 0; i < tc && tc < 5; i++) {
                uintptr_t tp2 = Ptr(tl + 0x10 + (uintptr_t)i * 0x8);
                if (!tp2 || tp2 == local) continue;
                uintptr_t th3 = Ptr(Ptr(Ptr(tp2 + Off::P_HP1) + Off::P_HP2) + Off::P_HP3);
                TeamEntry te{};
                te.hp    = th3 ? ReadMem<float>(th3 + Off::P_HPVal) : 0.f;
                te.armor = ReadMem<float>(tp2 + Off::P_Armor);
                strncpy(te.name, ReadName(Ptr(tp2 + Off::P_Name)).c_str(), 19);
                team_list.push_back(te);
            }
        }

        // Düşman dict
        uintptr_t dict = Ptr(match + Off::Match_Dict);
        uintptr_t ents = dict ? Ptr(dict + Off::Dict_Ents) : 0;
        int32_t   cnt  = dict ? ReadMem<int32_t>(dict + Off::Dict_Cnt) : 0;

        std::vector<EspEntry> esp_list;
        Vec3  lock_tgt{};
        float lock_dist  = 1e9f;
        bool  lock_found = false;

        uintptr_t aim_info  = Ptr(local + Off::P_AimInfo);
        Vec3      aim_spawn = aim_info ? ReadMem<Vec3>(aim_info + Off::Aim_Pos) : Vec3{};

        for (int i = 0; i < cnt && cnt < 200; i++) {
            uintptr_t ep = Ptr(ents + 0x10 + (uintptr_t)i * Off::Dict_Str + Off::Dict_Val);
            if (!ep || ep == local) continue;
            if (ReadMem<uint32_t>(ep + Off::P_Dead)) continue;

            uintptr_t h3   = Ptr(Ptr(Ptr(ep + Off::P_HP1) + Off::P_HP2) + Off::P_HP3);
            float     hp   = h3 ? ReadMem<float>(h3 + Off::P_HPVal) : 0.f;
            float     armor= ReadMem<float>(ep + Off::P_Armor);
            if (hp <= 0.f) continue;

            if (g_feat[F_GHOSTWALL]) GhostWall(ep);

            Vec3 wh  = BonePos(Ptr(ep + Off::B_Head));
            Vec3 wbr = BonePos(Ptr(ep + Off::B_Breast));
            Vec3 whi = BonePos(Ptr(ep + Off::B_Hip));
            Vec3 wls = BonePos(Ptr(ep + Off::B_LSho));
            Vec3 wrs = BonePos(Ptr(ep + Off::B_RSho));
            Vec3 wlh = BonePos(Ptr(ep + Off::B_LHand));
            Vec3 wrh = BonePos(Ptr(ep + Off::B_RHand));
            Vec3 wla = BonePos(Ptr(ep + Off::B_LAnk));
            Vec3 wra = BonePos(Ptr(ep + Off::B_RAnk));

            float dist = (wh - local_head).len();

            auto proj = [&](Vec3 w, BP& bp) -> bool {
                Vec2 s{};
                bool ok = W2S(vp, w, s, SW, SH);
                bp.sx = s.x;
                bp.sy = s.y;
                return ok;
            };

            EspEntry e{};
            bool vis = true;
            vis &= proj(wh,  e.head);
            vis &= proj(wbr, e.breast);
            vis &= proj(whi, e.hip);
            vis &= proj(wls, e.l_sho);
            vis &= proj(wrs, e.r_sho);
            vis &= proj(wlh, e.l_hand);
            vis &= proj(wrh, e.r_hand);
            vis &= proj(wla, e.l_ank);
            vis &= proj(wra, e.r_ank);
            if (!vis) continue;

            float bh  = e.r_ank.sy - e.head.sy;
            e.box_top = e.head.sy   - bh * 0.1f;
            e.box_bot = e.r_ank.sy  + bh * 0.05f;
            e.box_cx  = e.head.sx;
            e.box_w   = (e.box_bot - e.box_top) * 0.42f;
            e.hp      = hp;
            e.armor   = armor;
            e.dist    = dist;
            e.is_bot  = (uint8_t)ReadMem<uint32_t>(ep + Off::P_Bot);
            strncpy(e.name, ReadName(Ptr(ep + Off::P_Name)).c_str(), 23);
            esp_list.push_back(e);

            if (dist < lock_dist) {
                lock_dist  = dist;
                lock_tgt   = wh;
                lock_found = true;
            }
        }

        // Item ESP
        std::vector<ItemEntry> item_list;
        if (g_feat[F_ITEMESP] || g_feat[F_LOOTFILTER]) {
            uintptr_t im = Ptr(match + Off::ItemManager);
            uintptr_t il = im ? Ptr(im + Off::ItemList) : 0;
            int32_t   ic = im ? ReadMem<int32_t>(im + Off::ItemCount) : 0;
            for (int i = 0; i < ic && ic < 500; i++) {
                uintptr_t item = Ptr(il + 0x10 + (uintptr_t)i * 0x8);
                if (!item) continue;
                Vec3  wpos = ReadMem<Vec3>(item + Off::ItemPos);
                float dist = (wpos - local_head).len();
                if (dist > 150.f) continue;
                Vec2 s{};
                if (!W2S(vp, wpos, s, SW, SH)) continue;
                uint8_t type = (uint8_t)ReadMem<uint32_t>(item + Off::ItemType);
                if (g_feat[F_LOOTFILTER] && type != 0 && type != 2) continue;
                ItemEntry ie{};
                ie.sx   = s.x;
                ie.sy   = s.y;
                ie.dist = dist;
                ie.type = type;
                item_list.push_back(ie);
            }
        }

        // Vehicle
        std::vector<VehEntry> veh_list;
        if (g_feat[F_VEHICLESP] || g_feat[F_VEHSPEED]) {
            uintptr_t vm = Ptr(match + Off::VehicleManager);
            uintptr_t vl = vm ? Ptr(vm + Off::VehicleList) : 0;
            int32_t   vc = vm ? ReadMem<int32_t>(vm + Off::VehicleCount) : 0;
            for (int i = 0; i < vc && vc < 50; i++) {
                uintptr_t veh = Ptr(vl + 0x10 + (uintptr_t)i * 0x8);
                if (!veh) continue;
                if (g_feat[F_VEHSPEED]) {
                    float spd = ReadMem<float>(veh + Off::VehicleSpeed);
                    if (spd > 0.f && spd < 100.f)
                        WriteMem<float>(veh + Off::VehicleSpeed, spd * 2.f);
                }
                if (g_feat[F_VEHICLESP]) {
                    Vec3  wpos = ReadMem<Vec3>(veh + Off::VehiclePos);
                    float dist = (wpos - local_head).len();
                    if (dist > 300.f) continue;
                    Vec2 s{};
                    if (!W2S(vp, wpos, s, SW, SH)) continue;
                    VehEntry ve{};
                    ve.sx   = s.x;
                    ve.sy   = s.y;
                    ve.dist = dist;
                    ve.hp   = ReadMem<float>(veh + Off::VehicleHP);
                    veh_list.push_back(ve);
                }
            }
        }

        // ── FEATURE APPLY ──
        if (g_feat[F_NORECOIL])    NoRecoil(local);
        if (g_feat[F_RAPIDFIRE])   RapidFire(local);
        if (g_feat[F_NOSWAY])      NoSway(local);
        if (g_feat[F_BULLETSPD])   BulletSpeed(local);
        if (g_feat[F_NOSPREAD])    NoSpread(local);
        if (g_feat[F_INFINITEAMMO])InfiniteAmmo(local);
        if (g_feat[F_NOZOOM])      NoZoom(local);
        if (g_feat[F_FASTRELOAD])  FastReload(local);
        if (g_feat[F_MAXHP])       MaxHP(local);
        if (g_feat[F_MAXARMOR])    MaxArmor(local);
        if (g_feat[F_AUTOHEAL])    AutoHeal(local);
        if (g_feat[F_NOKNOCKBACK]) NoKnockback(local);
        if (g_feat[F_NOFALLDAM])   NoFallDmg(local);
        if (g_feat[F_SPEED1])      SpeedHack(local, 1.5f);
        if (g_feat[F_SPEED2])      SpeedHack(local, 2.0f);
        if (g_feat[F_SPEED3])      SpeedHack(local, 3.0f);
        if (g_feat[F_FLYHACK])     FlyHack(local);
        if (g_feat[F_PARASPEED])   ParaSpeed(local);
        if (g_feat[F_ANTIAIM])     AntiAIM(local);
        if (g_feat[F_WIDEFOV])     WideFOV(local);
        if (g_feat[F_NOGRASS])     NoGrass(facade);
        if (g_feat[F_BRIGHTMODE])  BrightMode(facade);
        if (g_feat[F_NIGHTMODE])   NightMode(facade);

        if (lock_found) {
            if (g_feat[F_AIMBOT])
                Aimbot(local, local_head, lock_tgt);
            else if (g_feat[F_CAMLOCK])
                CamLock(local, local_head, lock_tgt);

            if (g_feat[F_SILENTAIM] &&
                ReadMem<uint32_t>(local + Off::P_Firing) &&
                aim_info) {
                Vec3 dir = (lock_tgt - aim_spawn).norm();
                std::uniform_int_distribution<int> sk(1, 3);
                static int sc = 0;
                if (++sc % sk(rng) == 0)
                    WriteMem<Vec3>(aim_info + Off::Aim_Dir, dir);
            }
        }

        // ── ESP WRITE ──
        {
            FILE* ef = fopen(ESP_PATH, "wb");
            if (ef) {
                int32_t en = (int32_t)esp_list.size();
                int32_t in = (int32_t)item_list.size();
                int32_t vn = (int32_t)veh_list.size();
                int32_t tn = (int32_t)team_list.size();
                fwrite(&en,           4, 1, ef);
                fwrite(&in,           4, 1, ef);
                fwrite(&vn,           4, 1, ef);
                fwrite(&tn,           4, 1, ef);
                fwrite(&local_hp,     4, 1, ef);
                fwrite(&local_armor,  4, 1, ef);
                fwrite(&szinfo,  sizeof(SafeZoneInfo), 1,  ef);
                fwrite(esp_list.data(),  sizeof(EspEntry),  en, ef);
                fwrite(item_list.data(), sizeof(ItemEntry),  in, ef);
                fwrite(veh_list.data(),  sizeof(VehEntry),   vn, ef);
                fwrite(team_list.data(), sizeof(TeamEntry),  tn, ef);
                fclose(ef);
            }
        }

        JSlp(8, 4);

    } // while(true)

} // main
