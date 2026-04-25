"use client";
import {useEffect} from "react";
import {useRouter} from "next/navigation";

export default function Careers (){
    const router = useRouter();
    useEffect(() => {
          const user = localStorage.getItem("user");
        if(!user){
                router.replace("/login");
                }
        },[]);


    return <div> Careers page </div>;
}