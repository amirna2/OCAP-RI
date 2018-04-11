/*
 ESS Extremely Simple Serialization for C++

 http://www.novadsp.com/ess

 Copyright (c) Jerry Evans, 2008-2009
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.

 */

#ifndef ESS_RTTI_H
#define ESS_RTTI_H

//-----------------------------------------------------------------------------
#include <map>
#include <string>
//#include <strstream>
#include <sstream>

namespace ess
{

//-----------------------------------------------------------------------------
//
template<typename T>
struct root
{
    enum
    {
        value
    };
};

//-----------------------------------------------------------------------------
// ESS class factory interface.
template<typename Root>
class IFactory
{
public:
    virtual Root* build() = 0;
    virtual unsigned long* checker() = 0;
};

//-----------------------------------------------------------------------------
// ESS class factory. Used to create new instances on the heap.
// Requires a void constructor
template<typename Derived, typename Root>
class CFactory: public IFactory<Root>
{
public:
    // this creates the new instance
    virtual Root* build()
    {
        //
        Root* p = new Derived();
        return p;
    }
    // used at registration to check reregistration is harmless,
    // i.e. class name is associated with same factory instance
    virtual unsigned long* checker()
    {
        static unsigned long dw;
        return &dw;
    }
};

//-----------------------------------------------------------------------------
// One templated class registry for each ESS root
// manages a map of templated factory functions which will create
// a new typed class instance given a class name
template<typename Root>
class class_registry
{
    // root class name as debugging aid
    std::string m_rootname;
    // map class names to factories
    std::map<std::string, IFactory<Root>*> m_mapper;
    typedef typename std::map<std::string, IFactory<Root>*>::iterator
            factory_iterator;
public:

    // constructor takes root name of the hierarchy to simplify debugging
    class_registry(const char* rootname) :
        m_rootname(rootname)
    {
    }

    // constructor takes root name of the hierarchy to simplify debugging
    ~class_registry()
    {
        // clean up
        for (factory_iterator fit = m_mapper.begin(); fit != m_mapper.end(); ++fit)
        {
            delete fit->second;
        }
        //
        m_mapper.clear();
    }

    const char* rootname() const
    {
        return m_rootname.c_str();
    }
    // register a new classname and factory
    bool Register(const char* classname, IFactory<Root>* pFactory)
    {
        factory_iterator fit = m_mapper.find(classname);
        bool ret = (fit == m_mapper.end());
        if (!ret)
        {
            // re-registration - same factory?
            ret = (pFactory->checker() == fit->second->checker());
            // check to see if associated factory is different
            if (!ret)
            {
                // no - different factory.
                std::stringstream strs;
                strs << "Duplicate registration of " << classname
                        << " in root registry " << m_rootname;
                //throw std::exception(strs.str().c_str());
                throw std::exception();
            }
        }
        else
        {
            // map it
            m_mapper[classname] = pFactory;
        }
        //
        return ret;
    }
    // create a new instance given a class name
    Root* Create(const char* classname)
    {
        Root* p = 0;
        factory_iterator fit = m_mapper.find(classname);
        if (fit == m_mapper.end())
        {
            // did you register the classname?
            std::stringstream strs;
            strs << "Cannot find " << classname << " in root registry "
                    << m_rootname;
            //throw std::exception(strs.str().c_str());
            throw std::exception();
        }
        p = fit->second->build();
        if (p == 0)
        {
            std::stringstream strs;
            strs << "Failed to create instance of " << classname << " in root "
                    << m_rootname;
            //throw std::exception(strs.str().c_str());
            throw std::exception();
        }
        return p;
    }
};

//-----------------------------------------------------------------------------
// Virtual void base class used as a registration helper
class IRegistrar
{
public:
    virtual void Register() = 0;
    virtual const char* classname() const = 0;
};

//-----------------------------------------------------------------------------
// concrete implementation of interface used to register an ESS class
template<typename Derived, typename Root>
class class_registrar: public IRegistrar
{
    std::string m_classname;
public:
    class_registrar(const std::string& classname) :
        m_classname(classname)
    {
    }
    virtual ~class_registrar()
    {
    }
    virtual void Register()
    {
        // this gives us a compile time check that the root
        // is actually a root
        class_registry<Root>* pRegistry = Root::get_registry();
        //class_registry<Derived>* pRegistry = Derived::get_registry();
        pRegistry->Register(m_classname.c_str(), new CFactory<Derived, Root> ());
    }
    virtual const char* classname() const
    {
        return m_classname.c_str();
    }
};

//-----------------------------------------------------------------------------
// Helper functions to get runtime information from persistent classes
template<typename Type>
inline std::string name_from_instance(Type& arg)
{
    // this is a virtual call to get the correct
    // derived name which RTTI does not provide
    return arg.get_name();
}

//-----------------------------------------------------------------------------
// Helper functions to create a new instance. This is the critical element
// in the (re)construction of polymorphic types
template<typename Type>
inline Type*
instance_from_name(const char* classname)
{
    // since get registry is a static with a different signature
    // at each level of inheritance we can overload the function name
    ess::class_registry<Type>* p = Type::get_registry();
    // creates correct derived type or throws ...
    Type* pRet = p->Create(classname);
    // explicit return for debugging
    return pRet;
}

template<typename Type>
inline Type instance_from_type()
{
    return Type();
}

//-----------------------------------------------------------------------------
// All persistent classes that need to be registered can be done using the <<
// operator in the following way:
//
// Given a set of classes A, B and C
//
// Registry reg;
// reg << Registrar<A>() << Registrar<B>() << Registrar<C>()
//
// The overloaded << operator will called Register() in the Registrar() instance
//
class registry_manager
{
    // this is a master mapping
    std::map<std::string, void*> m_mapper;
    typedef std::map<std::string, void*>::iterator mapper_iterator;
public:
    //
    registry_manager& operator<<(const IRegistrar& arg)
    {
        IRegistrar* p = const_cast<IRegistrar*> (&arg);
        mapper_iterator mit = m_mapper.find(p->classname());
        if (mit != m_mapper.end())
        {

        }
        p->Register();
        return (*this);
    }
};

//-----------------------------------------------------------------------------
// Special variety - if you *do* have an *optional* common ancestry for
// all persistent class, you can use this templated registry which gives
// a free implementation of 'create-by-name'

template<typename T>
class typed_registry_manager
{
    // this is a master mapping
    std::map<std::string, void*> m_mapper;
    typedef std::map<std::string, void*>::iterator mapper_iterator;
public:
    //
    typed_registry_manager& operator<<(const IRegistrar& arg)
    {
        IRegistrar* p = const_cast<IRegistrar*> (&arg);
        mapper_iterator mit = m_mapper.find(p->classname());
        if (mit != m_mapper.end())
        {

        }
        p->Register();
        return (*this);
    }

    // i.e. the equivalent of MFC RUNTIMECLASS Create()
    T* Create(const char* classname)
    {
        //T* p = 0;
        return instance_from_name<T> (classname);
    }
};

//-----------------------------------------------------------------------------
// templated inline function that is called by the ESS_ROOT macro implementation
template<typename Root>
inline class_registry<Root>* get_registry_impl(const char* rootname)
{
    // when this function is called the registry for the
    // hierarchy based on T is created and will last
    // for the duration of the program run.
    static ess::class_registry<Root> registry(rootname);
    return &registry;
}

//-----------------------------------------------------------------------------
// Yet another helper to detect errors at compile time. This is specifically
// aimed at checking for incorrect derivations in the ESS_RTTI macro. It
// *cannot* detect pathological cases where the same error occurs in both macros
template<typename Derived, typename Root>
struct compile_time_checker
{
    compile_time_checker()
    {
        // assignment of one anonymous instance to another
        // if types do not match then you get a compile time error
        // either because you failed to define ESS_ROOT or
        // the second argument to ESS_RTTI does not match that
        // used in ESS_ROOT
        //Root::ess_root() = ess::root<Root>();
        typename Root::ess_root() = ess::root<Root>();
    }
};

//-----------------------------------------------------------------------------
// Another templated inline that does the casting for us
template<typename Derived, typename Root>
inline class_registry<Derived>*
get_registry_root(const char* rootname)
{
    // compile time check to ensure correct derivation
    compile_time_checker<Derived, Root> ();
    // so we are actually getting the registry for the root class ...
    // which is static and therefore exists in only one place in the
    // hierarchy
    return reinterpret_cast<class_registry<Derived>*> (get_registry_impl<Root> (
            rootname));
}
//-----------------------------------------------------------------------------
// this is probably overkill but it makes instrumenting a bit simpler.
// we use the static idiom to create a single instance on a per-class basis
template<typename T>
inline
const char* get_name_impl(const char* classname)
{
    static std::string s_classname(classname);
    return s_classname.c_str();
}

//-----------------------------------------------------------------------------
// this defines an enumeration that exists only once in the root class
// to maximise protection declare it as protected. the name may collide ...
#define ESS_ROOT(rootname)  typedef ess::root<rootname> ess_root;

//-----------------------------------------------------------------------------
// macros much more palatable if they hand off to real code you can step through
// in a debugger
// this macro adds a friend declaration so the runtime code can create instances
// with private and protected constructors
#define ESS_RTTI(classname,rootname)\
friend class ess::CFactory<classname,rootname>; \
friend class classname ess::instance_from_type<classname>();\
virtual const char* get_name() { return ess::get_name_impl<classname>(#classname); }\
static ess::class_registry<classname>* get_registry() { return ess::get_registry_root<classname,rootname>(#rootname); }

//-----------------------------------------------------------------------------
// and finally a little macro for registration
#define ESS_REGISTER(classname,rootname)\
ess::class_registrar<classname,rootname>(#classname)

//-----------------------------------------------------------------------------

} // end of ess namespace

#endif
//-----------------------------------------------------------------------------
